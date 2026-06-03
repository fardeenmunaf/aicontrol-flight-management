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
import pilot.domain.PilotCompanyAssignment;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AddPilotToCompanyUI extends AbstractUI {

    private JpaPilotCompanyAssignmentRepository assignments() {
        return new JpaPilotCompanyAssignmentRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    protected boolean doShow() {
        try {
            final AirTransportCompanyId companyId = resolveCompanyId();
            if (companyId == null) return true;

            final Set<String> alreadyAssigned = new HashSet<>();
            assignments().findAll().forEach(a -> alreadyAssigned.add(a.pilotUserId().toString()));

            final List<Pilot> available = new ArrayList<>();
            PersistenceContext.repositories().pilots().findAll()
                    .forEach(p -> {
                        if (!alreadyAssigned.contains(p.userId().toString())) {
                            available.add(p);
                        }
                    });

            if (available.isEmpty()) {
                System.out.println("No unassigned pilots available.");
                return true;
            }

            System.out.println();
            System.out.printf("%-4s %-35s %-10s %-30s%n", "No.", "EMAIL", "STATUS", "CERTIFIED MODELS");
            System.out.println("-".repeat(82));
            for (int i = 0; i < available.size(); i++) {
                final Pilot p = available.get(i);
                final String models = p.certifiedAircraftModels().stream()
                        .map(m -> m.modelName()).sorted().collect(Collectors.joining(", "));
                System.out.printf("%-4d %-35s %-10s %-30s%n",
                        i + 1, p.userId().toString(), p.status().name(),
                        models.isEmpty() ? "—" : models);
            }
            System.out.println();

            final String raw = Console.readLine("Select pilot number to add (0 to cancel)");
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
            if (choice < 1 || choice > available.size()) {
                System.out.println("Invalid selection.");
                return true;
            }

            final Pilot selected = available.get(choice - 1);
            assignments().save(new PilotCompanyAssignment(selected.userId(), companyId));
            System.out.println("Pilot " + selected.userId() + " added to your company's roster.");

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
        return "Add Pilot to My Company";
    }
}
