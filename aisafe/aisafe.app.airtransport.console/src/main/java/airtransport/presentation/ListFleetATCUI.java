package airtransport.presentation;

import aircraft.application.ListCompanyFleetController;
import aircraft.dto.AircraftDTO;
import airtransportcompany.domain.AirTransportCompany;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import shared.domain.UserId;

import java.util.List;

public class ListFleetATCUI extends AbstractUI {

    private final ListCompanyFleetController controller = new ListCompanyFleetController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().customerCollaborators());

    @Override
    protected boolean doShow() {
        try {
            final var session = AuthzRegistry.authorizationService().session().orElseThrow(IllegalStateException::new);
            final var userId = UserId.valueOf(session.authenticatedUser().identity().toString());
            final CustomerCollaborator collaborator = PersistenceContext.repositories().customerCollaborators()
                    .findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Current user is not a customer collaborator"));

            if (collaborator.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
                System.out.println("You are not assigned to an air transport company.");
                return true;
            }

            final AirTransportCompany company = PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(collaborator.airTransportCompanyId())
                    .orElseThrow(() -> new IllegalStateException("Air transport company not found"));

            final List<AircraftDTO> fleet = controller.listCompanyFleet();

            System.out.println("\n=== Fleet for: " + company.name() + " ===\n");

            if (fleet.isEmpty()) {
                System.out.println("No aircraft registered for this company.");
                return true;
            }

            System.out.printf("%-12s | %-25s | %-12s | %-45s | %-10s%n",
                    "REGISTRATION", "MODEL", "STATUS", "CABIN CONFIGURATION", "COUNTRY");
            System.out.println("-".repeat(120));

            for (final AircraftDTO aircraft : fleet) {
                System.out.printf("%-12s | %-25s | %-12s | %-45s | %-10s%n",
                        aircraft.registrationNumber,
                        aircraft.model,
                        aircraft.operationalStatus,
                        aircraft.cabinConfig,
                        aircraft.registrationCountry);
            }

            System.out.println("-".repeat(120));
            System.out.printf("Total Aircraft: %d%n", fleet.size());
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() {
        return "List Fleet";
    }
}



