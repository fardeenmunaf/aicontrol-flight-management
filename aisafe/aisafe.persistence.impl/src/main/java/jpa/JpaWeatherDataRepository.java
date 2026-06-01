package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;
import shared.domain.WeatherDataId;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of WeatherDataRepository.
 * Provides ORM-based persistence for WeatherData aggregates.
 *
 * @author LAPR4 Team
 */
class JpaWeatherDataRepository
        extends BaseJpaRepositoryBase<WeatherData, WeatherDataId, WeatherDataId>
        implements WeatherDataRepository {

    public JpaWeatherDataRepository(final TransactionalContext autoTx) {
        super(autoTx, "weatherDataId");
    }

    public JpaWeatherDataRepository(final String persistenceUnitName) {
        super("weatherDataId");
    }

    @Override
    public Optional<WeatherData> findByAirControlAreaAndDateTime(String area, LocalDateTime dateTime) {
        if (area == null || dateTime == null) {
            return Optional.empty();
        }

        final String normalizedArea = area.trim();
        for (final WeatherData wd : findAll()) {
            if (normalizedArea.equals(wd.acaCode())
                    && wd.date() != null
                    && wd.date().equals(dateTime.toLocalDate())
                    && wd.startHour() != null
                    && wd.endHour() != null) {
                final var time = dateTime.toLocalTime();
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
        for (final WeatherData wd : findAll()) {
            if (normalizedArea.equals(wd.acaCode())) {
                result.add(wd);
            }
        }

        return result;
    }
}

