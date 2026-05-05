package weather.presentation.weatherData;

import eapli.framework.presentation.console.AbstractUI;
import weatherData.application.ListWeatherDataController;
import weatherData.dto.WeatherDataDTO;

public class ViewWeatherDataUI extends AbstractUI {

    private final ListWeatherDataController controller = new ListWeatherDataController();

    @Override
    protected boolean doShow() {
        final Iterable<WeatherDataDTO> records = controller.listAll();

        int count = 0;
        for (final WeatherDataDTO record : records) {

            System.out.printf(
                    "Area: %s | DateTime: %s | Wind Speed: %.2f | Wind Direction: %.2f | Temp: %.2f | Pressure: %.2f%n",
                    record.airControlArea(),
                    record.dateTime(),
                    record.windSpeed(),
                    record.windDirection(),
                    record.temperature(),
                    record.pressure()
            );

            count++;
        }

        if (count == 0) {
            System.out.println("No weather data found.");
        }

        return true;
    }

    @Override
    public String headline() {
        return "View Weather Data";
    }
}