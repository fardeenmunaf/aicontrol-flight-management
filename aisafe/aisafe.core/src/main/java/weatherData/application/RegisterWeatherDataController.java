package weatherData.application;

import infrastructure.persistence.PersistenceContext;
import weatherData.domain.WeatherData;
import weatherData.dto.WeatherDataDTO;
import weatherData.repository.WeatherDataRepository;

public class RegisterWeatherDataController {

    private final WeatherDataRepository repo = PersistenceContext.repositories().weatherData();

    public boolean registerWeatherData(final WeatherDataDTO dto) {

        // validações básicas
        if (dto.airControlArea() == null || dto.airControlArea().isBlank()) {
            throw new IllegalArgumentException("Air control area cannot be empty");
        }

        if (dto.windSpeed() < 0) {
            throw new IllegalArgumentException("Wind speed must be non-negative");
        }

        if (dto.windDirection() < 0 || dto.windDirection() > 360) {
            throw new IllegalArgumentException("Wind direction must be between 0 and 360");
        }

        if (dto.pressure() <= 0) {
            throw new IllegalArgumentException("Pressure must be positive");
        }

        if (dto.temperature() < -100 || dto.temperature() > 100) {
            throw new IllegalArgumentException("Temperature out of range");
        }

        // criar entidade (DOMAIN)
        WeatherData data = new WeatherData(
                dto.airControlArea(),
                dto.dateTime(),
                dto.windDirection(),
                dto.windSpeed(),
                dto.temperature(),
                dto.pressure());

        // guardar no repo
        repo.save(data);

        return true;
    }
}