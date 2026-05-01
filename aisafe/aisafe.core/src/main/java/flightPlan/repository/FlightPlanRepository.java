package flightPlan.repository;

import eapli.framework.domain.repositories.DomainRepository;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanId;
import flightPlan.domain.FlightPlanStatus;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository interface for FlightPlan aggregate.
 */
public interface FlightPlanRepository extends DomainRepository<FlightPlanId, FlightPlan> {

    Optional<FlightPlan> findByFlightPlanId(FlightPlanId flightPlanId);

    Iterable<FlightPlan> findByStatus(FlightPlanStatus status);

    Iterable<FlightPlan> findByDepartureBetween(Instant start, Instant end);
}

