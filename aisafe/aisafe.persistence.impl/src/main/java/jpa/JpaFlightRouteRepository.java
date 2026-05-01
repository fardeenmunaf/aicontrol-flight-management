package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.FlightRouteId;
import flightRoute.domain.RouteDesignation;
import flightRoute.repository.FlightRouteRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of FlightRouteRepository.
 */
class JpaFlightRouteRepository extends BaseJpaRepositoryBase<FlightRoute, FlightRouteId, FlightRouteId>
        implements FlightRouteRepository {

    public JpaFlightRouteRepository(final TransactionalContext autoTx) {
        super(autoTx, "flightRouteId");
    }

    public JpaFlightRouteRepository(final String persistenceUnitName) {
        super("flightRouteId");
    }

    @Override
    public Optional<FlightRoute> findByDesignation(final RouteDesignation designation) {
        final Map<String, Object> params = new HashMap<>();
        params.put("designation", designation);
        return matchOne("e.designation = :designation", params);
    }

    @Override
    public Iterable<FlightRoute> findActiveOn(final LocalDate date) {
        final Map<String, Object> params = new HashMap<>();
        params.put("date", date);
        return match("e.activeFrom <= :date AND (e.activeUntil IS NULL OR e.activeUntil >= :date)", params);
    }
}

