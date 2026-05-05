package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.RouteDesignation;
import flightRoute.repository.FlightRouteRepository;
import shared.domain.FlightRouteId;

import java.time.LocalDate;
import java.util.Optional;

/**
 * In-memory implementation of FlightRouteRepository.
 */
public class InMemoryFlightRouteRepository extends InMemoryDomainRepository<FlightRoute, FlightRouteId>
        implements FlightRouteRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<FlightRoute> findByDesignation(final RouteDesignation designation) {
        return matchOne(fr -> fr.designation().equals(designation));
    }

    @Override
    public Iterable<FlightRoute> findActiveOn(final LocalDate date) {
        return match(fr -> fr.isActiveOn(date));
    }
}

