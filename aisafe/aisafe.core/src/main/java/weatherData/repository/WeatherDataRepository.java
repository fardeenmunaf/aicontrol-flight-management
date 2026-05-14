package weatherData.repository;

import eapli.framework.domain.repositories.DomainRepository;
import weatherData.domain.WeatherData;
import shared.domain.WeatherDataId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for WeatherData aggregate.
 * Defines domain-specific persistence operations for WeatherData entities.
 *
 * @author LAPR4 Team
 */
public interface WeatherDataRepository extends DomainRepository<WeatherDataId, WeatherData> {

    Optional<WeatherData> findByAirControlAreaAndDateTime(
            String area, LocalDateTime dateTime);

    List<WeatherData> findByAirControlArea(String area);
}
