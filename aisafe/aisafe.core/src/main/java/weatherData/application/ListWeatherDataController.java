package weatherData.application;

import infrastructure.persistence.PersistenceContext;
import weatherData.domain.WeatherData;
import weatherData.dto.WeatherDataDTO;
import weatherData.repository.WeatherDataRepository;

import java.util.ArrayList;
import java.util.List;

public class ListWeatherDataController {

    private final WeatherDataRepository repo =
            PersistenceContext.repositories().weatherData();

    public Iterable<WeatherDataDTO> listAll() {

        Iterable<WeatherData> data = repo.findAll();
        List<WeatherDataDTO> dtos = new ArrayList<>();

        for (WeatherData wd : data) {
            dtos.add(new WeatherDataDTO(
                    wd.acaCode(),
                    wd.latitude1(),
                    wd.longitude1(),
                    wd.latitude2(),
                    wd.longitude2(),
                    wd.altitudeInf(),
                    wd.altitudeSup(),
                    wd.direction(),
                    wd.windSpeed(),
                    wd.date(),
                    wd.startHour(),
                    wd.endHour()
            ));
        }

        return dtos;
    }

    /**
     * List weather data for a given air control area and date.
     * If area is null or empty, lists for all areas on the given date.
     */
    public Iterable<WeatherDataDTO> listByDateAndArea(final String area, final java.time.LocalDate date) {

        final List<WeatherDataDTO> dtos = new ArrayList<>();

        Iterable<WeatherData> data;
        if (area == null || area.trim().isEmpty()) {
            data = repo.findAll();
        } else {
            data = repo.findByAirControlArea(area);
        }

        for (WeatherData wd : data) {
            // If date filter is null, include all records (only area filter applies)
            if (date == null || (wd.date() != null && wd.date().equals(date))) {
                dtos.add(new WeatherDataDTO(
                        wd.acaCode(),
                        wd.latitude1(),
                        wd.longitude1(),
                        wd.latitude2(),
                        wd.longitude2(),
                        wd.altitudeInf(),
                        wd.altitudeSup(),
                        wd.direction(),
                        wd.windSpeed(),
                        wd.date(),
                        wd.startHour(),
                        wd.endHour()
                ));
            }
        }

        return dtos;
    }
}