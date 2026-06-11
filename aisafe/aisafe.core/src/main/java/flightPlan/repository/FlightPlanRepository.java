package flightPlan.repository;

import eapli.framework.domain.repositories.DomainRepository;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import shared.domain.FlightPlanId;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository interface for FlightPlan aggregate.
 */
public interface FlightPlanRepository extends DomainRepository<FlightPlanId, FlightPlan> {

    Optional<FlightPlan> findByFlightPlanId(FlightPlanId flightPlanId);

    Iterable<FlightPlan> findByStatus(FlightPlanStatus status);

    /** Plans on the given route reference departing strictly after the given instant. */
    Iterable<FlightPlan> findByRouteIdAndDepartureAfter(String routeId, Instant departure);

    Iterable<FlightPlan> findByPilotRef(String pilotRef);

    Iterable<FlightPlan> findByFlightDesignator(String flightDesignator);
}

