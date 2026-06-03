package weatherData.application;

import infrastructure.persistence.PersistenceContext;
import weatherData.domain.WeatherData;
import weatherData.dto.WeatherDataDTO;
import weatherData.repository.WeatherDataRepository;

public class RegisterWeatherDataController {

    private final WeatherDataRepository repo = PersistenceContext.repositories().weatherData();

    public boolean registerWeatherData(final WeatherDataDTO dto) {

        // validações básicas
        if (dto.acaCode() == null || dto.acaCode().isBlank()) {
            throw new IllegalArgumentException("ACA code cannot be empty");
        }

        if (dto.latitude1() < -90 || dto.latitude1() > 90) {
            throw new IllegalArgumentException("Latitude 1 must be between -90 and 90");
        }

        if (dto.latitude2() < -90 || dto.latitude2() > 90) {
            throw new IllegalArgumentException("Latitude 2 must be between -90 and 90");
        }

        if (dto.longitude1() < -180 || dto.longitude1() > 180) {
            throw new IllegalArgumentException("Longitude 1 must be between -180 and 180");
        }

        if (dto.longitude2() < -180 || dto.longitude2() > 180) {
            throw new IllegalArgumentException("Longitude 2 must be between -180 and 180");
        }

        if (dto.altitudeInf() < 0) {
            throw new IllegalArgumentException("Altitude inferior must be non-negative");
        }

        if (dto.altitudeSup() < 0) {
            throw new IllegalArgumentException("Altitude superior must be non-negative");
        }

        if (dto.altitudeInf() > dto.altitudeSup()) {
            throw new IllegalArgumentException("Altitude inferior must be less than or equal to altitude superior");
        }

        if (dto.direction() < 0 || dto.direction() > 360) {
            throw new IllegalArgumentException("Direction must be between 0 and 360 degrees");
        }

        if (dto.windSpeed() < 0) {
            throw new IllegalArgumentException("Wind speed must be non-negative");
        }

        if (dto.startHour().isAfter(dto.endHour())) {
            throw new IllegalArgumentException("Start hour must be before or equal to end hour");
        }

        // criar entidade (DOMAIN)
        WeatherData data = new WeatherData(
                dto.acaCode(),
                dto.latitude1(),
                dto.longitude1(),
                dto.latitude2(),
                dto.longitude2(),
                dto.altitudeInf(),
                dto.altitudeSup(),
                dto.direction(),
                dto.windSpeed(),
                dto.date(),
                dto.startHour(),
                dto.endHour());

        // guardar no repo
        repo.save(data);

        return true;
    }
}