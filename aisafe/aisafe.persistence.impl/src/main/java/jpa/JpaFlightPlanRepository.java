package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.repository.FlightPlanRepository;
import shared.domain.FlightPlanId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of FlightPlanRepository.
 */
class JpaFlightPlanRepository extends BaseJpaRepositoryBase<FlightPlan, FlightPlanId, FlightPlanId>
        implements FlightPlanRepository {

    public JpaFlightPlanRepository(final TransactionalContext autoTx) {
        super(autoTx, "flightPlanId");
    }

    public JpaFlightPlanRepository(final String persistenceUnitName) {
        super("flightPlanId");
    }

    @Override
    public Optional<FlightPlan> findByFlightPlanId(final FlightPlanId flightPlanId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("flightPlanId", flightPlanId);
        return matchOne("e.flightPlanId = :flightPlanId", params);
    }

    @Override
    public Iterable<FlightPlan> findByStatus(final FlightPlanStatus status) {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        return match("e.status = :status", params);
    }

    @Override
    public Iterable<FlightPlan> findByDepartureBetween(final Instant start, final Instant end) {
        final Map<String, Object> params = new HashMap<>();
        params.put("start", start);
        params.put("end", end);
        return match("e.departureDateTime BETWEEN :start AND :end", params);
    }

    @Override
    public Iterable<FlightPlan> findByRouteIdAndDepartureAfter(final String routeId, final Instant departure) {
        final Map<String, Object> params = new HashMap<>();
        params.put("routeId", routeId);
        params.put("departure", departure);
        return match("e.flightRouteId = :routeId AND e.departureDateTime > :departure", params);
    }
}

