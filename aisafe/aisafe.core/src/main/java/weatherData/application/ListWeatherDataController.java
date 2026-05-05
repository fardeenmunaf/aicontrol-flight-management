package weatherData.application;

import eapli.framework.infrastructure.persistence.PersistenceContext;
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
                    wd.airControlArea(),
                    wd.dateTime(),
                    wd.windDirection(),
                    wd.windSpeed(),
                    wd.temperature(),
                    wd.pressure()
            ));
        }

        return dtos;
    }
}