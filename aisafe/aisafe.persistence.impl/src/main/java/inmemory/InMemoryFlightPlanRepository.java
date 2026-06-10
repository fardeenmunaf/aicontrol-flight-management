package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.repository.FlightPlanRepository;
import shared.domain.FlightPlanId;

import java.time.Instant;
import java.util.Optional;

/**
 * In-memory implementation of FlightPlanRepository.
 */
public class InMemoryFlightPlanRepository extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
        implements FlightPlanRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<FlightPlan> findByFlightPlanId(final FlightPlanId flightPlanId) {
        return matchOne(fp -> fp.identity().equals(flightPlanId));
    }

    @Override
    public Iterable<FlightPlan> findByStatus(final FlightPlanStatus status) {
        return match(fp -> fp.status() == status);
    }

    @Override
    public Iterable<FlightPlan> findByRouteIdAndDepartureAfter(final String routeId, final Instant departure) {
        return match(fp -> routeId.equals(fp.routeReference())
                && fp.departureTime() != null
                && fp.departureTime().isAfter(departure));
    }
}

