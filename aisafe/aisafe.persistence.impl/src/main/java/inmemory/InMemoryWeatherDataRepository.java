package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;
import shared.domain.WeatherDataId;

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
}

