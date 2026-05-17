package airtransport.presentation;

import aircraft.application.ListCompanyFleetController;
import aircraft.dto.AircraftDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

import java.util.List;
import java.util.stream.Collectors;

public class ListFleetByModelUI extends AbstractUI {

    private final ListCompanyFleetController controller = new ListCompanyFleetController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().customerCollaborators());

    @Override
    protected boolean doShow() {
        try {
            final String modelFilter = Console.readLine("Enter model name to search");
            if (modelFilter == null || modelFilter.isBlank()) {
                System.out.println("Model name cannot be empty.");
                return true;
            }

            final List<AircraftDTO> fleet = controller.listCompanyFleet();
            final String lower = modelFilter.trim().toLowerCase();
            final List<AircraftDTO> filtered = fleet.stream()
                    .filter(a -> a.model.toLowerCase().contains(lower))
                    .collect(Collectors.toList());

            System.out.println("\n=== Fleet filtered by Model: \"" + modelFilter.trim() + "\" ===\n");

            if (filtered.isEmpty()) {
                System.out.println("No aircraft found matching model \"" + modelFilter.trim() + "\".");
                return true;
            }

            System.out.printf("%-12s | %-25s | %-12s | %-45s | %-10s%n",
                    "REGISTRATION", "MODEL", "STATUS", "CABIN CONFIGURATION", "COUNTRY");
            System.out.println("-".repeat(120));

            for (final AircraftDTO aircraft : filtered) {
                System.out.printf("%-12s | %-25s | %-12s | %-45s | %-10s%n",
                        aircraft.registrationNumber,
                        aircraft.model,
                        aircraft.operationalStatus,
                        aircraft.cabinConfig,
                        aircraft.registrationCountry);
            }

            System.out.println("-".repeat(120));
            System.out.printf("Total Aircraft: %d%n", filtered.size());
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() {
        return "List Fleet By Model";
    }
}
