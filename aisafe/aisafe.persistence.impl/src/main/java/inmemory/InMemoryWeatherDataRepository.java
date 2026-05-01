package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;

/**
 * In-memory implementation of WeatherDataRepository.
 * Used for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemoryWeatherDataRepository extends InMemoryDomainRepository<WeatherData, Long>
        implements WeatherDataRepository {

    static {
        InMemoryInitializer.init();
    }
}

