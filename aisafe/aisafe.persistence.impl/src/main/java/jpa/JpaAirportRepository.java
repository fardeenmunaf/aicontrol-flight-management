package jpa;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
import eapli.framework.domain.repositories.TransactionalContext;

import java.util.Map;
import java.util.Optional;

public class JpaAirportRepository
        extends BaseJpaRepositoryBase<Airport, Long, Long>
        implements AirportRepository {

    public JpaAirportRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaAirportRepository(final String persistenceUnitName) {
        super("id");
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