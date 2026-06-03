package flightRoute.application;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import auth.domain.AISafeRoles;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.RouteDesignation;
import flightRoute.repository.FlightRouteRepository;
import shared.domain.FlightRouteId;
import shared.domain.UserId;

import java.time.LocalDate;

public class CreateFlightRouteController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightRouteRepository flightRouteRepository;
    private final AirportRepository airportRepository;
    private final CustomerCollaboratorRepository collaboratorRepository;
    private final AirTransportCompanyRepository companyRepository;

    public CreateFlightRouteController(final FlightRouteRepository flightRouteRepository,
                                       final AirportRepository airportRepository,
                                       final CustomerCollaboratorRepository collaboratorRepository,
                                       final AirTransportCompanyRepository companyRepository) {
        this.flightRouteRepository = flightRouteRepository;
        this.airportRepository = airportRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.companyRepository = companyRepository;
    }

    public FlightRoute createFlightRoute(final String designationStr,
                                         final String originIataCode,
                                         final String destinationIataCode,
                                         final LocalDate activeFrom) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final var session = authz.session().orElseThrow(IllegalStateException::new);
        final UserId userId = UserId.valueOf(session.authenticatedUser().identity().toString());

        final CustomerCollaborator collaborator = collaboratorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Logged in user is not a customer collaborator"));

        if (collaborator.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
            throw new IllegalStateException("Logged in user is not assigned to an air transport company");
        }

        final AirTransportCompany company = companyRepository
                .ofIdentity(collaborator.airTransportCompanyId())
                .orElseThrow(() -> new IllegalStateException("Air transport company not found"));

        final RouteDesignation designation = new RouteDesignation(designationStr);

        final String prefix = designationStr.substring(0, 2);
        if (!prefix.equalsIgnoreCase(company.iataCode())) {
            throw new IllegalArgumentException(
                    "Route designation must start with the company IATA code '" + company.iataCode() + "'");
        }

        if (flightRouteRepository.findByDesignation(designation).isPresent()) {
            throw new IllegalArgumentException(
                    "A route with designation '" + designationStr + "' already exists");
        }

        if (originIataCode == null || originIataCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Origin airport IATA code must be provided");
        }
        if (destinationIataCode == null || destinationIataCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination airport IATA code must be provided");
        }

        final String originCode = originIataCode.trim().toUpperCase();
        final String destCode = destinationIataCode.trim().toUpperCase();

        if (originCode.equals(destCode)) {
            throw new IllegalArgumentException("Origin and destination airports must be different");
        }

        final Airport origin = airportRepository.findByIataCode(originCode)
                .orElseThrow(() -> new IllegalArgumentException("Origin airport not found: " + originCode));
        final Airport destination = airportRepository.findByIataCode(destCode)
                .orElseThrow(() -> new IllegalArgumentException("Destination airport not found: " + destCode));

        if (activeFrom == null) {
            throw new IllegalArgumentException("Active from date must be provided");
        }

        final FlightRoute route = new FlightRoute(
                FlightRouteId.newId(), designation, activeFrom, origin, destination, company);
        return flightRouteRepository.save(route);
    }
}
