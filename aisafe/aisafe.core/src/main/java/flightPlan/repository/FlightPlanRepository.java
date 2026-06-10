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

    // TODO: flight plan data is now stored as a single JSON blob, so route reference and
    // departure time are no longer queryable columns. This method currently returns nothing,
    // which DISABLES the "block route deactivation if future flights exist" safety check in
    // DeleteFlightRouteController. Re-implement once the DSL-based flight plan rework defines
    // the final JSON shape (or promote routeId/departure back to dedicated columns).
    Iterable<FlightPlan> findByRouteIdAndDepartureAfter(String routeId, Instant departure);
}

