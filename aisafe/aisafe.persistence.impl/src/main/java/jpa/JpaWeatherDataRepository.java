package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;

/**
 * JPA implementation of WeatherDataRepository.
 * Provides ORM-based persistence for WeatherData aggregates.
 *
 * @author LAPR4 Team
 */
class JpaWeatherDataRepository
        extends BaseJpaRepositoryBase<WeatherData, Long, Long>
        implements WeatherDataRepository {

    public JpaWeatherDataRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaWeatherDataRepository(final String persistenceUnitName) {
        super("id");
    }
}

