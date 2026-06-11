package weather.presentation.weatherData;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import weather.ClientSession;
import weatherData.application.RegisterWeatherDataController;
import weatherData.dto.WeatherDataDTO;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


public class RegisterWeatherDataUI extends AbstractUI {

    @Override
    public boolean doShow() {
        try {
            System.out.println("\n=== Register Weather Data ===");

            final String acaCode = readString("ACA Code");
            final Double latitude1 = readDouble("Latitude 1 (degrees)");
            final Double longitude1 = readDouble("Longitude 1 (degrees)");
            final Double latitude2 = readDouble("Latitude 2 (degrees)");
            final Double longitude2 = readDouble("Longitude 2 (degrees)");
            final Double altitudeInf = readDouble("Altitude Inferior (ft)");
            final Double altitudeSup = readDouble("Altitude Superior (ft)");
            final Double direction = readDouble("Wind Direction (degrees)");
            final Double windSpeed = readDouble("Wind Speed (knots)");
            final LocalDate date = readDate("Date (YYYY-MM-DD)");
            final LocalTime startHour = readTime("Start Hour (HH:MM)");
            final LocalTime endHour = readTime("End Hour (HH:MM)");

            WeatherDataDTO dto = new WeatherDataDTO(
                    acaCode,
                    latitude1,
                    longitude1,
                    latitude2,
                    longitude2,
                    altitudeInf,
                    altitudeSup,
                    direction,
                    windSpeed,
                    date,
                    startHour,
                    endHour
            );

            if (ClientSession.getMode() == ClientSession.ConnectionMode.DIRECT_DB) {
                RegisterWeatherDataController controller = new RegisterWeatherDataController();
                controller.registerWeatherData(dto);
                System.out.println("Weather data registered successfully (Direct DB).");
            } else {
                try {
                    String csvPayload = serializeWeatherDataDTO(dto);

                    String response = ClientSession.getClient().uploadWeather(csvPayload);
                    System.out.println("Server response: " + response);
                } catch (IOException e) {
                    System.out.println("Error communicating with server: " + e.getMessage());
                }
            }

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

    private String readString(final String prompt) {
        while (true) {
            try {
                final String input = Console.readLine(prompt);
                if (input == null || input.isBlank()) {
                    throw new IllegalArgumentException("Input cannot be empty");
                }
                return input.trim();
            } catch (final IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }
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

    private LocalDate readDate(final String prompt) {
        while (true) {
            try {
                final String input = Console.readLine(prompt);
                if (input == null || input.isBlank()) {
                    throw new IllegalArgumentException("Input cannot be empty");
                }
                return LocalDate.parse(input);
            } catch (final DateTimeParseException ex) {
                System.out.println("Invalid date. Please use format YYYY-MM-DD.");
            } catch (final IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private LocalTime readTime(final String prompt) {
        while (true) {
            try {
                final String input = Console.readLine(prompt);
                if (input == null || input.isBlank()) {
                    throw new IllegalArgumentException("Input cannot be empty");
                }
                return LocalTime.parse(input);
            } catch (final DateTimeParseException ex) {
                System.out.println("Invalid time. Please use format HH:MM.");
            } catch (final IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private String serializeWeatherDataDTO(WeatherDataDTO dto) {
        return new StringJoiner(";")
                .add(dto.acaCode())
                .add(String.valueOf(dto.latitude1()))
                .add(String.valueOf(dto.longitude1()))
                .add(String.valueOf(dto.latitude2()))
                .add(String.valueOf(dto.longitude2()))
                .add(String.valueOf(dto.altitudeInf()))
                .add(String.valueOf(dto.altitudeSup()))
                .add(String.valueOf(dto.direction()))
                .add(String.valueOf(dto.windSpeed()))
                .add(dto.date().toString())
                .add(dto.startHour().toString())
                .add(dto.endHour().toString())
                .toString();
    }

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        for (final T item : iterable) {
            list.add(item);
        }
        return list;
    }

}