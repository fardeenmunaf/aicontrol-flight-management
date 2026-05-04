package airport.repositories;

import airport.domain.Airport;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

public interface AirportRepository extends DomainRepository<Long, Airport> {

    Optional<Airport> findByIataCode(String iataCode);

    Optional<Airport> findByIcaoCode(String icaoCode);
}