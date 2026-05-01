package weatherData.repository;

import eapli.framework.domain.repositories.DomainRepository;
import weatherData.domain.WeatherData;

/**
 * Repository interface for WeatherData aggregate.
 * Defines domain-specific persistence operations for WeatherData entities.
 *
 * @author LAPR4 Team
 */
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
    // Domain-specific query methods can be added here as needed
}

