package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;
import shared.domain.WeatherDataId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Test-scoped in-memory implementation of WeatherDataRepository.
 * Placed under test sources so PersistenceContext can load it via
 * Class.forName("inmemory.InMemoryRepositoryFactory") during tests.
 */
public class InMemoryWeatherDataRepository extends InMemoryDomainRepository<WeatherData, WeatherDataId>
        implements WeatherDataRepository {

    // no additional behavior required for saving; implement query methods
    // declared in the WeatherDataRepository interface.

    @Override
    public Optional<WeatherData> findByAirControlAreaAndDateTime(final String area, final LocalDateTime dateTime) {
        if (area == null || dateTime == null) {
            return Optional.empty();
        }
        for (final WeatherData wd : this.findAll()) {
            if (area.equals(wd.airControlArea()) && dateTime.equals(wd.dateTime())) {
                return Optional.of(wd);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<WeatherData> findByAirControlArea(final String area) {
        final List<WeatherData> result = new ArrayList<>();
        if (area == null) {
            return result;
        }
        for (final WeatherData wd : this.findAll()) {
            if (area.equals(wd.airControlArea())) {
                result.add(wd);
            }
        }
        return result;
    }

    // no static initializer required for tests
}



