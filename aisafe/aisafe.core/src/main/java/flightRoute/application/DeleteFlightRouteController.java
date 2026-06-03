package flightRoute.application;

import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import auth.domain.AISafeRoles;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightPlan.repository.FlightPlanRepository;
import flightRoute.domain.FlightRoute;
import flightRoute.repository.FlightRouteRepository;
import shared.domain.FlightRouteId;
import shared.domain.UserId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class DeleteFlightRouteController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightRouteRepository flightRouteRepository;
    private final FlightPlanRepository flightPlanRepository;
    private final CustomerCollaboratorRepository collaboratorRepository;
    private final AirTransportCompanyRepository companyRepository;

    public DeleteFlightRouteController(final FlightRouteRepository flightRouteRepository,
                                       final FlightPlanRepository flightPlanRepository,
                                       final CustomerCollaboratorRepository collaboratorRepository,
                                       final AirTransportCompanyRepository companyRepository) {
        this.flightRouteRepository = flightRouteRepository;
        this.flightPlanRepository = flightPlanRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.companyRepository = companyRepository;
    }

    public void deleteFlightRoute(final String routeId, final LocalDate deactivationDate) {
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

        final FlightRoute route = flightRouteRepository.ofIdentity(FlightRouteId.valueOf(routeId))
                .orElseThrow(() -> new IllegalArgumentException("Flight route not found"));

        if (!route.company().identity().equals(company.identity())) {
            throw new IllegalStateException("Flight route does not belong to your company");
        }

        if (deactivationDate == null) {
            throw new IllegalArgumentException("Deactivation date must be provided");
        }

        final Instant cutoff = deactivationDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Iterable<flightPlan.domain.FlightPlan> plannedFlights =
                flightPlanRepository.findByRouteIdAndDepartureAfter(routeId, cutoff);
        if (plannedFlights.iterator().hasNext()) {
            throw new IllegalStateException(
                    "Cannot deactivate route: there are planned flights after " + deactivationDate);
        }

        route.deactivateFrom(deactivationDate);
        flightRouteRepository.save(route);
    }
}
