package backoffice.presentation.airport;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airport.application.RegisterAirportController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

public class RegisterAirportUI extends AbstractUI {

    private final RegisterAirportController controller =
            new RegisterAirportController(
                    PersistenceContext.repositories().airports(),
                    PersistenceContext.repositories().airControlAreas());

    private final AirControlAreaRepository airControlAreaRepository =
            PersistenceContext.repositories().airControlAreas();

    @Override
    protected boolean doShow() {
        try {
            final String name = Console.readLine("Airport name");
            final String town = Console.readLine("Town");
            final String country = Console.readLine("Country");
            final String iataCode = Console.readLine("IATA code (3 letters)");
            final String icaoCode = Console.readLine("ICAO code (4 letters)");
            final Double elevation = readDouble("Elevation (meters)");
            final Double latitude = readDouble("Latitude");
            final Double longitude = readDouble("Longitude");

            System.out.println("Existing Air Control Areas:");
            for (final AirControlArea area : airControlAreaRepository.findAll()) {
                System.out.println(area.identity());
            }

            final String airControlAreaId = Console.readLine("Air Control Area ID");

            controller.registerAirport(
                    name, town, country, iataCode, icaoCode,
                    elevation, latitude, longitude, airControlAreaId);

            System.out.println("Airport registered successfully.");
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Register Airport";
    }

    private Double readDouble(final String prompt) {
        final String raw = Console.readLine(prompt);
        try {
            return Double.parseDouble(raw.trim());
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number: " + raw);
        }
    }
}