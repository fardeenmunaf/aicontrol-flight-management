package flightRoute.repository;

import eapli.framework.domain.repositories.DomainRepository;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.FlightRouteId;
import flightRoute.domain.RouteDesignation;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository interface for FlightRoute aggregate.
 */
public interface FlightRouteRepository extends DomainRepository<FlightRouteId, FlightRoute> {

    Optional<FlightRoute> findByDesignation(RouteDesignation designation);

    Iterable<FlightRoute> findActiveOn(LocalDate date);
}

