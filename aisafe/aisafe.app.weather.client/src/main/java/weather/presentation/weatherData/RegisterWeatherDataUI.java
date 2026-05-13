package weather.presentation.weatherData;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import weatherData.application.RegisterWeatherDataController;
import weatherData.dto.WeatherDataDTO;



public class RegisterWeatherDataUI extends AbstractUI {


    private final RegisterWeatherDataController controller = new RegisterWeatherDataController();
    private final AirControlAreaRepository airControlAreaRepository =
            PersistenceContext.repositories().airControlAreas();

    @Override
    public boolean doShow() {
        try {
            final String airControlArea = selectAirControlArea();
            if (airControlArea == null) {
                return false;
            }
            final LocalDateTime dateTime = readDateTime();
            final Double windDirection = readDouble("Wind Direction (degrees)");
            final Double windSpeed = readDouble("Wind Speed (m/s)");
            final Double temperature = readDouble("Temperature (C)");
            final Double pressure = readDouble("Pressure (hPa)");

            WeatherDataDTO dto = new WeatherDataDTO(
                    airControlArea,
                    dateTime,
                    windDirection,
                    windSpeed,
                    temperature,
                    pressure
            );

            controller.registerWeatherData(dto);
            System.out.println("Weather data registered successfully.");
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }

    }

    @Override
    public String headline() {
        return "Register Weather Data";
    }

    private String selectAirControlArea() {
        final List<AirControlArea> areas = toList(airControlAreaRepository.findAll());

        if (areas.isEmpty()) {
            System.out.println("No air control areas available.");
            return null;
        }

        System.out.println("\nAvailable Air Control Areas:");
        for (int i = 0; i < areas.size(); i++) {
            final AirControlArea area = areas.get(i);
            System.out.printf("%d - Code: %s | MinFuel: %.2f | Boundaries: %s%n",
                    i + 1,
                    area.areaCode(),
                    area.minimumRequiredArrivalFuel(),
                    area.geographicBoundaries());
        }

        final int choice = readInteger();
        if (choice < 1 || choice > areas.size()) {
            throw new IllegalArgumentException("Invalid air control area option");
        }

        return areas.get(choice - 1).areaCode();
    }

    private Double readDouble(final String prompt) {
        while (true) {
            try {
                final String input = Console.readLine(prompt);
                if (input == null || input.isBlank()) {
                    throw new IllegalArgumentException("Input cannot be empty");
                }
                return Double.parseDouble(input);
            } catch (final NumberFormatException ex) {
                System.out.println("Invalid number. Please enter a valid numeric value.");
            } catch (final IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private LocalDateTime readDateTime() {
        while (true) {
            try {
                final String input = Console.readLine("Date/Time (YYYY-MM-DDTHH:MM)");
                if (input == null || input.isBlank()) {
                    throw new IllegalArgumentException("Input cannot be empty");
                }
                return LocalDateTime.parse(input);
            } catch (final DateTimeParseException ex) {
                System.out.println("Invalid date/time. Please use format YYYY-MM-DDTHH:MM.");
            } catch (final IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private int readInteger() {
        while (true) {
            try {
                final String input = Console.readLine("Choose one air control area");
                if (input == null || input.isBlank()) {
                    throw new IllegalArgumentException("Input cannot be empty");
                }
                return Integer.parseInt(input);
            } catch (final NumberFormatException ex) {
                System.out.println("Invalid number. Please enter a valid integer value.");
            } catch (final IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
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