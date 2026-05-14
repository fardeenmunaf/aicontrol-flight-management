package backoffice.presentation.airtransportcompany;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import airtransportcompany.application.RegisterAirTransportCompanyController;
import infrastructure.persistence.PersistenceContext;

public class RegisterAirTransportCompanyUI extends AbstractUI {

    private final RegisterAirTransportCompanyController controller =
            new RegisterAirTransportCompanyController(
                    PersistenceContext.repositories().airTransportCompanies());

    @Override
    protected boolean doShow() {
        try {
            final String name = Console.readLine("Company name");
            final String iataCode = Console.readLine("IATA code (2 letters)");
            final String icaoCode = Console.readLine("ICAO code (2-3 letters)");

            controller.registerCompany(name, iataCode, icaoCode);

            System.out.println("\nAir Transport Company registered successfully.");
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("\nError: " + ex.getMessage());
            return false;
        } catch (final Exception ex) {
            System.out.println("\nError registering air transport company: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Register New Air Transport Company";
    }
}

