package jpa;

import java.util.Map;
import java.util.Optional;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import shared.domain.AirportId;

public class JpaAirportRepository
        extends BaseJpaRepositoryBase<Airport, AirportId, AirportId>
        implements AirportRepository {

    public JpaAirportRepository(final TransactionalContext autoTx) {
        super(autoTx, "airportId");
    }

    public JpaAirportRepository(final String persistenceUnitName) {
        super("airportId");
    }

    @Override
    public Optional<Airport> findByIataCode(final String iataCode) {
        return matchOne("LOWER(e.iataCode) = LOWER(:iataCode)", Map.of("iataCode", iataCode));
    }

    @Override
    public Optional<Airport> findByIcaoCode(final String icaoCode) {
        return matchOne("LOWER(e.icaoCode) = LOWER(:icaoCode)", Map.of("icaoCode", icaoCode));
    }
}