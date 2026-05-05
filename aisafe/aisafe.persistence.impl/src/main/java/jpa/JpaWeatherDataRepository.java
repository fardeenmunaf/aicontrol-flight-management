package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;
import shared.domain.WeatherDataId;

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
}

