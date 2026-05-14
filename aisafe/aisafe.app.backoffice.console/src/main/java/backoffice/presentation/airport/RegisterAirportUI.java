package backoffice.presentation.airport;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airport.application.RegisterAirportController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

            final String airControlAreaId = resolveAirControlArea(latitude, longitude);
            if (airControlAreaId == null) {
                return false;
            }

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

    private String resolveAirControlArea(final double latitude, final double longitude) {
        final Optional<AirControlArea> detected = controller.findAreaForCoordinates(latitude, longitude);

        if (detected.isPresent()) {
            final AirControlArea area = detected.get();
            System.out.printf("Air Control Area detected automatically: %s%n", area);
            return area.identity().toString();
        }

        System.out.println("No air control area found for these coordinates. Please select manually:");
        return selectManually();
    }

    private String selectManually() {
        final List<AirControlArea> areas = toList(airControlAreaRepository.findAll());
        if (areas.isEmpty()) {
            System.out.println("No air control areas available.");
            return null;
        }
        for (int i = 0; i < areas.size(); i++) {
            System.out.printf("%d - %s%n", i + 1, areas.get(i));
        }
        final int choice = Console.readInteger("Choose an Air Control Area: ");
        if (choice < 1 || choice > areas.size()) {
            System.out.println("Invalid option.");
            return null;
        }
        return areas.get(choice - 1).identity().toString();
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

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        for (final T item : iterable) {
            list.add(item);
        }
        return list;
    }
}
