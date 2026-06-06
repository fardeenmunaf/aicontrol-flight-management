package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;
import shared.domain.WeatherDataId;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory implementation of WeatherDataRepository.
 * Used for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemoryWeatherDataRepository extends InMemoryDomainRepository<WeatherData, WeatherDataId>
        implements WeatherDataRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<WeatherData> findByAirControlAreaAndDateTime(String area, LocalDateTime dateTime) {
        if (area == null || dateTime == null) {
            return Optional.empty();
        }

        final String normalizedArea = area.trim();
        for (final WeatherData wd : this.findAll()) {
            if (normalizedArea.equals(wd.acaCode())
                    && wd.date() != null
                    && wd.date().equals(dateTime.toLocalDate())
                    && wd.startHour() != null
                    && wd.endHour() != null) {
                final LocalTime time = dateTime.toLocalTime();
                if (!time.isBefore(wd.startHour()) && !time.isAfter(wd.endHour())) {
                    return Optional.of(wd);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public List<WeatherData> findByAirControlArea(String area) {
        final List<WeatherData> result = new ArrayList<>();
        if (area == null) {
            return result;
        }

        final String normalizedArea = area.trim();
        for (final WeatherData wd : this.findAll()) {
            if (normalizedArea.equals(wd.acaCode())) {
                result.add(wd);
            }
        }

        return result;
    }
}
