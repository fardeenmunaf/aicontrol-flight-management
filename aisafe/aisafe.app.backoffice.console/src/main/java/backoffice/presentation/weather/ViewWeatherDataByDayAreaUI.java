package backoffice.presentation.weather;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import weatherData.application.ListWeatherDataController;
import weatherData.dto.WeatherDataDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ViewWeatherDataByDayAreaUI extends AbstractUI {

    private final ListWeatherDataController controller = new ListWeatherDataController();

    @Override
    protected boolean doShow() {
        System.out.println("\n=== Backoffice: Consult Weather Data by Day and Area ===");

        final String area = Console.readLine("Enter Air Control Area (ACA) code (leave blank for all): ");
        final String dateStr = Console.readLine("Enter Date (dd/MM/yyyy) to filter by (leave blank for all): ");

        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final LocalDate date;
        if (dateStr == null || dateStr.trim().isEmpty()) {
            // Allow blank date to filter by all dates
            date = null;
        } else {
            try {
                date = LocalDate.parse(dateStr.trim(), fmt);
            } catch (final DateTimeParseException ex) {
                System.out.println("Invalid date format. Expected dd/MM/yyyy. Aborting.");
                return false;
            }
        }

        Iterable<WeatherDataDTO> records = controller.listByDateAndArea(area == null ? null : area.trim(), date);

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
        return "Backoffice: View Weather Data by Day and Area";
    }
}

