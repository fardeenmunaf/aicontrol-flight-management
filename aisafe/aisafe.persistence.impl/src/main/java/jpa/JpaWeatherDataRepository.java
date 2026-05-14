package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;
import shared.domain.WeatherDataId;

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
        return Optional.empty();
    }

    @Override
    public List<WeatherData> findByAirControlArea(String area) {
        return List.of();
    }
}

