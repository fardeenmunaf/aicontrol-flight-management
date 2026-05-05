package weatherData.repository;

import eapli.framework.domain.repositories.DomainRepository;
import weatherData.domain.WeatherData;
import shared.domain.WeatherDataId;

import java.util.List;

/**
 * Repository interface for WeatherData aggregate.
 * Defines domain-specific persistence operations for WeatherData entities.
 *
 * @author LAPR4 Team
 */
public interface WeatherDataRepository extends DomainRepository<WeatherDataId, WeatherData> {


}

