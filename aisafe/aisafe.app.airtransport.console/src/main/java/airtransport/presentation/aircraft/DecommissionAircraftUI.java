package airtransport.presentation.aircraft;

import aircraft.application.DecommissionAircraftController;
import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import airtransportcompany.domain.AirTransportCompany;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import infrastructure.persistence.PersistenceContext;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.List;

public class DecommissionAircraftUI extends AbstractUI {

        private final DecommissionAircraftController controller = new DecommissionAircraftController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().airTransportCompanies(),
            PersistenceContext.repositories().flights());

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

            System.out.println("Decommissioning aircraft from: " + company.name());
            System.out.println();

            // Load all aircraft and filter for this company (excluding already decommissioned)
            final List<Aircraft> activeAircraft = new ArrayList<>();
            PersistenceContext.repositories().aircraft().findAll().forEach(a -> {
                if (a.airTransportCompany() != null 
                    && a.airTransportCompany().identity().equals(company.identity())
                    && a.operationStatus() != AircraftOperationStatus.DECOMISSIONED) {
                    activeAircraft.add(a);
                }
            });

            if (activeAircraft.isEmpty()) {
                System.out.println("No active aircraft available to decommission in your fleet.");
                return true;
            }

            System.out.println("Active aircraft in your fleet:");
            for (int i = 0; i < activeAircraft.size(); i++) {
                final Aircraft a = activeAircraft.get(i);
                System.out.printf("%d) Registration: %s | Country: %s | Model: %s | Status: %s%n",
                        i + 1, a.registrationNumber(), a.registrationCountry(), 
                        a.aircraftModel().modelName(), a.operationStatus());
            }

            final int choice = readInt("Select aircraft number to decommission");
            if (choice < 1 || choice > activeAircraft.size()) {
                throw new IllegalArgumentException("Invalid aircraft option");
            }

            final Aircraft selectedAircraft = activeAircraft.get(choice - 1);
            
            System.out.println();
            System.out.println("Aircraft to decommission:");
            System.out.printf("  Registration: %s (%s)%n", selectedAircraft.registrationNumber(), selectedAircraft.registrationCountry());
            System.out.printf("  Model: %s%n", selectedAircraft.aircraftModel().modelName());
            System.out.printf("  Current Status: %s%n", selectedAircraft.operationStatus());
            System.out.println();

            final String confirm = Console.readLine("Are you sure you want to decommission this aircraft? (yes/no)");
            if (confirm == null || !confirm.trim().equalsIgnoreCase("yes")) {
                System.out.println("Decommissioning cancelled.");
                return true;
            }

            controller.decommissionAircraft(selectedAircraft.identity().toString());
            System.out.println("Aircraft decommissioned successfully.");

        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() {
        return "Decommission Aircraft";
    }

    private int readInt(final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null) {
            throw new IllegalArgumentException("Input was interrupted or no value provided");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number: " + raw);
        }
    }
}
