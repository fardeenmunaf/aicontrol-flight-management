package airport.repositories;

import java.util.Optional;

import airport.domain.Airport;
import eapli.framework.domain.repositories.DomainRepository;
import shared.domain.AirportId;

public interface AirportRepository extends DomainRepository<AirportId, Airport> {

    Optional<Airport> findByIataCode(String iataCode);

    Optional<Airport> findByIcaoCode(String icaoCode);
}