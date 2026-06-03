package airtransport.presentation.pilot;

import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import jpa.JpaPilotCompanyAssignmentRepository;
import jpa.PersistenceSettings;
import pilot.domain.Pilot;
import pilot.domain.PilotStatus;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ActivateCompanyPilotUI extends AbstractUI {

    private JpaPilotCompanyAssignmentRepository assignments() {
        return new JpaPilotCompanyAssignmentRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    protected boolean doShow() {
        try {
            final AirTransportCompanyId companyId = resolveCompanyId();
            if (companyId == null) return true;

            final Set<String> myPilotIds = assignments().findByCompanyId(companyId).stream()
                    .map(a -> a.pilotUserId().toString())
                    .collect(Collectors.toSet());

            final List<Pilot> inactivePilots = new ArrayList<>();
            PersistenceContext.repositories().pilots().findAll()
                    .forEach(p -> {
                        if (myPilotIds.contains(p.userId().toString())
                                && p.status() == PilotStatus.INACTIVE) {
                            inactivePilots.add(p);
                        }
                    });

            if (inactivePilots.isEmpty()) {
                System.out.println("No inactive pilots in your roster.");
                return true;
            }

            System.out.println();
            System.out.printf("%-4s %-35s %-30s%n", "No.", "EMAIL", "CERTIFIED MODELS");
            System.out.println("-".repeat(72));
            for (int i = 0; i < inactivePilots.size(); i++) {
                final Pilot p = inactivePilots.get(i);
                final String models = p.certifiedAircraftModels().stream()
                        .map(m -> m.modelName()).sorted().collect(Collectors.joining(", "));
                System.out.printf("%-4d %-35s %-30s%n",
                        i + 1, p.userId().toString(),
                        models.isEmpty() ? "—" : models);
            }
            System.out.println();

            final String raw = Console.readLine("Select pilot number to activate (0 to cancel)");
            final int choice;
            try {
                choice = Integer.parseInt(raw.trim());
            } catch (final NumberFormatException e) {
                System.out.println("Invalid input.");
                return true;
            }

            if (choice == 0) {
                System.out.println("Cancelled.");
                return true;
            }
            if (choice < 1 || choice > inactivePilots.size()) {
                System.out.println("Invalid selection.");
                return true;
            }

            final Pilot selected = inactivePilots.get(choice - 1);

            final String confirm = Console.readLine(
                    "Activate pilot " + selected.userId() + "? (Y/N)");
            if (!"Y".equalsIgnoreCase(confirm.trim())) {
                System.out.println("Cancelled.");
                return true;
            }

            selected.activate();
            PersistenceContext.repositories().pilots().save(selected);

            System.out.println("Pilot " + selected.userId() + " has been activated.");

        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    private AirTransportCompanyId resolveCompanyId() {
        final String email = AuthzRegistry.authorizationService().session()
                .map(s -> s.authenticatedUser().username().toString())
                .orElse(null);
        if (email == null) {
            System.out.println("No active session.");
            return null;
        }
        final CustomerCollaborator collab = PersistenceContext.repositories()
                .customerCollaborators().findByUserId(UserId.valueOf(email))
                .orElse(null);
        if (collab == null || collab.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
            System.out.println("Your account is not linked to an air transport company.");
            return null;
        }
        return collab.airTransportCompanyId();
    }

    @Override
    public String headline() {
        return "Activate Pilot";
    }
}
