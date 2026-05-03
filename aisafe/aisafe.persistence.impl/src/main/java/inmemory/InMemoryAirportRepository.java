package inmemory;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;

public class InMemoryAirportRepository
        extends InMemoryDomainRepository<Airport, Long>
        implements AirportRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<Airport> findByIataCode(final String iataCode) {
        return matchOne(a -> a.iataCode().equalsIgnoreCase(iataCode));
    }

    @Override
    public Optional<Airport> findByIcaoCode(final String icaoCode) {
        return matchOne(a -> a.icaoCode().equalsIgnoreCase(icaoCode));
    }
}