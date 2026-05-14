package backoffice.presentation.airtransportcompany;

import aircraft.application.ListCompanyFleetController;
import aircraft.dto.AircraftDTO;
import airtransportcompany.application.ListAirTransportCompaniesController;
import airtransportcompany.domain.AirTransportCompany;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

import java.util.List;

public class ListFleetATCUI extends AbstractUI {

    private final ListAirTransportCompaniesController companiesController =
            new ListAirTransportCompaniesController(
                    PersistenceContext.repositories().airTransportCompanies());

    private final ListCompanyFleetController fleetController =
            new ListCompanyFleetController(
                    PersistenceContext.repositories().aircraft(),
                    PersistenceContext.repositories().customerCollaborators());

    @Override
    protected boolean doShow() {
        try {
            // Get all companies
            final List<AirTransportCompany> companies = companiesController.allCompanies();

            if (companies.isEmpty()) {
                System.out.println("\n>>> No Air Transport Companies registered in the system.");
                return true;
            }

            // Display companies list
            System.out.println("\n=== Select Air Transport Company to View Fleet ===\n");
            int index = 1;
            for (final AirTransportCompany company : companies) {
                System.out.printf("%d. %s (%s - %s)%n", index, company.name(),
                        company.iataCode(), company.icaoCode());
                index++;
            }
            System.out.println("0. Return");

            // Get user choice
            final int choice = Console.readInteger("Enter your choice");

            if (choice == 0) {
                return true;
            }

            if (choice < 1 || choice > companies.size()) {
                System.out.println("\n>>> Invalid option.");
                return false;
            }

            // Get selected company
            final AirTransportCompany selectedCompany = companies.get(choice - 1);

            // Display fleet for selected company
            displayFleet(selectedCompany);
            return false;

        } catch (final IllegalArgumentException ex) {
            System.out.println("\n>>> Error: " + ex.getMessage());
            return false;
        } catch (final Exception ex) {
            System.out.println("\n>>> Error listing fleet: " + ex.getMessage());
            return false;
        }
    }

    private void displayFleet(final AirTransportCompany company) {
        try {
            // Get the fleet for the selected company
            final List<AircraftDTO> fleet = fleetController.listCompanyFleetAsBackoffice(
                    company.identity().toString());

            System.out.println("\n=== Fleet for: " + company.name() + " ===\n");

            if (fleet.isEmpty()) {
                System.out.println("No aircraft registered for this company.");
                return;
            }

            // Print header
            System.out.printf("%-12s | %-25s | %-12s | %-45s | %-10s%n",
                    "REGISTRATION", "MODEL", "STATUS", "CABIN CONFIGURATION", "COUNTRY");
            System.out.println("-".repeat(120));

            // Print each aircraft
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

        } catch (final Exception ex) {
            System.out.println("\n>>> Error: " + ex.getMessage());
        }
    }

    @Override
    public String headline() {
        return "List Air Transport Company Fleet";
    }
}


