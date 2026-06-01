package weather.presentation.weatherData;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
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

        final String area = Console.readLine("Enter Air Control Area (ACA) code (leave blank for all): ");
        final String dateStr = Console.readLine("Enter Date (dd/MM/yyyy) to filter by (leave blank for all): ");

        Iterable<WeatherDataDTO> records;

        if (dateStr == null || dateStr.trim().isEmpty()) {
            // No date filter - if area provided, use repo.findByAirControlArea via controller
            if (area == null || area.trim().isEmpty()) {
                records = controller.listAll();
            } else {
                records = controller.listByDateAndArea(area.trim(), null);
            }
        } else {
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            try {
                final LocalDate date = LocalDate.parse(dateStr.trim(), fmt);
                records = controller.listByDateAndArea(area == null ? null : area.trim(), date);
            } catch (final DateTimeParseException ex) {
                System.out.println("Invalid date format. Expected dd/MM/yyyy. Aborting.");
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