package airtransport.presentation.pilot;

import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import jpa.JpaPilotCompanyAssignmentRepository;
import jpa.PersistenceSettings;
import pilot.domain.Pilot;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ListCompanyPilotsUI extends AbstractUI {

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

            final List<Pilot> companyPilots = new ArrayList<>();
            PersistenceContext.repositories().pilots().findAll()
                    .forEach(p -> {
                        if (myPilotIds.contains(p.userId().toString())) {
                            companyPilots.add(p);
                        }
                    });

            if (companyPilots.isEmpty()) {
                System.out.println("No pilots assigned to your company.");
                return true;
            }

            System.out.println();
            System.out.printf("%-35s %-10s %-30s%n", "EMAIL", "STATUS", "CERTIFIED MODELS");
            System.out.println("-".repeat(78));
            for (final Pilot p : companyPilots) {
                final String models = p.certifiedAircraftModels().stream()
                        .map(m -> m.modelName()).sorted().collect(Collectors.joining(", "));
                System.out.printf("%-35s %-10s %-30s%n",
                        p.userId().toString(), p.status().name(),
                        models.isEmpty() ? "—" : models);
            }
            System.out.println();
            System.out.println(companyPilots.size() + " pilot(s) in your roster.");

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
        return "My Company's Pilot Roster";
    }
}
