package weather.presentation.weatherData;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import weatherData.application.RegisterWeatherDataController;
import weatherData.dto.WeatherDataDTO;



public class RegisterWeatherDataUI extends AbstractUI {


    private final RegisterWeatherDataController controller = new RegisterWeatherDataController();

    @Override
    public boolean doShow() {
        try {
            final String airControlArea = Console.readLine("Air Control Area");
            final LocalDateTime dateTime = readDateTime("Date/Time (YYYY-MM-DDTHH:MM)");
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

            controller.registerWeatherData(dto);;
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

    private Double readDouble(final String prompt) {
        while (true) {
            try {
                final String input = Console.readLine(prompt);
                return Double.parseDouble(input);
            } catch (final NumberFormatException ex) {
                System.out.println("Invalid number. Please enter a valid numeric value.");
            }
        }
    }

    private LocalDateTime readDateTime(final String prompt) {
        while (true) {
            try {
                final String input = Console.readLine(prompt);
                return LocalDateTime.parse(input);
            } catch (final DateTimeParseException ex) {
                System.out.println("Invalid date/time. Please use format YYYY-MM-DDTHH:MM.");
            }
        }
    }

}