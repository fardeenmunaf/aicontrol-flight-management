package weather.presentation.weatherData;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import weather.ClientSession;
import weatherData.application.ListWeatherDataController;
import weatherData.dto.WeatherDataDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ViewWeatherDataUI extends AbstractUI {

    private final ListWeatherDataController controller = new ListWeatherDataController();

    @Override
    protected boolean doShow() {
        System.out.println("\n=== Weather Data Records (filtered) ===");

        final String areaInput = Console.readLine("Enter Air Control Area (ACA) code (leave blank for all): ");
        final String area = (areaInput == null || areaInput.trim().isEmpty()) ? null : areaInput.trim();
        final String dateStr = Console.readLine("Enter Date (dd/MM/yyyy) to filter by (leave blank for all): ");

        Iterable<WeatherDataDTO> records;

        if (ClientSession.getMode() == ClientSession.ConnectionMode.DIRECT_DB) {
            if (dateStr == null || dateStr.trim().isEmpty()) {
                if (area == null) {
                    records = controller.listAll();
                } else {
                    records = controller.listByDateAndArea(area, null);
                }
            } else {
                final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                try {
                    final LocalDate date = LocalDate.parse(dateStr.trim(), fmt);
                    records = controller.listByDateAndArea(area, date);
                } catch (final DateTimeParseException ex) {
                    System.out.println("Invalid date format. Expected dd/MM/yyyy. Aborting.");
                    return false;
                }
            }
        } else {
            // TCP Mode
            try {
                String isoDate = null;
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    isoDate = LocalDate.parse(dateStr.trim(), fmt).toString(); // YYYY-MM-DD
                }
                String response = ClientSession.getClient().listWeather(area, isoDate);
                // O servidor responde com JSON do Iterable<WeatherDataDTO>
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                records = java.util.Arrays.asList(mapper.readValue(response, WeatherDataDTO[].class));
            } catch (Exception e) {
                System.out.println("Error fetching data from server: " + e.getMessage());
                return false;
            }
        }

        int count = 0;
        for (final WeatherDataDTO record : records) {
            System.out.printf(
                    "ACA: %s | Lat1: %.6f | Long1: %.6f | Lat2: %.6f | Long2: %.6f | " +
                    "Alt_inf: %.0f ft | Alt_sup: %.0f ft | Direction: %.2f° | WindSpeed: %.2f kts | " +
                    "Date: %s | Start: %s | End: %s%n",
                    record.acaCode(),
                    record.latitude1(),
                    record.longitude1(),
                    record.latitude2(),
                    record.longitude2(),
                    record.altitudeInf(),
                    record.altitudeSup(),
                    record.direction(),
                    record.windSpeed(),
                    record.date(),
                    record.startHour(),
                    record.endHour()
            );
            count++;
        }

        if (count == 0) {
            System.out.println("No weather data found for the given filters.");
        } else {
            System.out.println("\nTotal records: " + count);
        }

        return true;
    }

    @Override
    public String headline() {
        return "View Weather Data";
    }
}