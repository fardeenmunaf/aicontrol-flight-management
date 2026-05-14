package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;
import shared.domain.WeatherDataId;

import java.time.LocalDateTime;
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
        return Optional.empty();
    }

    @Override
    public List<WeatherData> findByAirControlArea(String area) {
        return List.of();
    }
}

