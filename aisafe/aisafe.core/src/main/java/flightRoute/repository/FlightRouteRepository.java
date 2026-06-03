package flightRoute.repository;

import eapli.framework.domain.repositories.DomainRepository;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.RouteDesignation;
import shared.domain.AirTransportCompanyId;
import shared.domain.FlightRouteId;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository interface for FlightRoute aggregate.
 */
public interface FlightRouteRepository extends DomainRepository<FlightRouteId, FlightRoute> {

    Optional<FlightRoute> findByDesignation(RouteDesignation designation);

    Iterable<FlightRoute> findActiveOn(LocalDate date);

    Iterable<FlightRoute> findByCompanyId(AirTransportCompanyId companyId);
}

