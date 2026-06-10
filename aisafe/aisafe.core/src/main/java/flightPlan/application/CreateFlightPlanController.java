package flightPlan.application;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.repositories.AircraftRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightPlan.domain.FlightPlan;
import flightPlan.repository.FlightPlanRepository;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.RouteDesignation;
import flightRoute.repository.FlightRouteRepository;
import pilot.domain.Pilot;
import pilot.domain.PilotCompanyAssignment;
import pilot.domain.PilotStatus;
import pilot.repository.PilotCompanyAssignmentRepository;
import pilot.repository.PilotRepository;
import shared.domain.AirTransportCompanyId;
import shared.domain.FlightPlanId;
import shared.domain.UserId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CreateFlightPlanController {

    private final FlightPlanRepository flightPlanRepository;
    private final FlightRouteRepository flightRouteRepository;
    private final AircraftRepository aircraftRepository;
    private final PilotRepository pilotRepository;
    private final PilotCompanyAssignmentRepository assignmentRepository;
    private final AirTransportCompanyRepository companyRepository;

    public CreateFlightPlanController(
            final FlightPlanRepository flightPlanRepository,
            final FlightRouteRepository flightRouteRepository,
            final AircraftRepository aircraftRepository,
            final PilotRepository pilotRepository,
            final PilotCompanyAssignmentRepository assignmentRepository,
            final AirTransportCompanyRepository companyRepository) {
        this.flightPlanRepository = Objects.requireNonNull(flightPlanRepository);
        this.flightRouteRepository = Objects.requireNonNull(flightRouteRepository);
        this.aircraftRepository = Objects.requireNonNull(aircraftRepository);
        this.pilotRepository = Objects.requireNonNull(pilotRepository);
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository);
        this.companyRepository = Objects.requireNonNull(companyRepository);
    }

    /**
     * Returns whether the logged-in pilot has a company assignment.
     * If false, the UI must ask the pilot to enter their company's IATA code.
     */
    public boolean pilotHasCompanyAssignment() {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        final String email = loggedInEmail();
        return assignmentRepository.findByPilotUserId(UserId.valueOf(email)).isPresent();
    }

    /**
     * Returns the IATA code of the pilot's company, or the provided hint if no assignment exists.
     */
    public String pilotCompanyIata(final String companyIataHint) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        final AirTransportCompany company = resolveCompany(UserId.valueOf(loggedInEmail()), companyIataHint);
        return company.iataCode();
    }

    /**
     * Returns all non-decommissioned aircraft belonging to the pilot's company.
     */
    public List<Aircraft> activeAircraftForPilot(final String companyIataHint) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        final AirTransportCompany company = resolveCompany(UserId.valueOf(loggedInEmail()), companyIataHint);
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : aircraftRepository.findByAirTransportCompanyId(company.identity())) {
            if (a.operationStatus() != AircraftOperationStatus.DECOMISSIONED) {
                result.add(a);
            }
        }
        return result;
    }

    /**
     * Returns all active routes for the pilot's company (or the given IATA hint).
     */
    public List<FlightRoute> activeRoutesForPilot(final String companyIataHint) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        final AirTransportCompany company = resolveCompany(UserId.valueOf(loggedInEmail()), companyIataHint);
        final List<FlightRoute> routes = new ArrayList<>();
        for (final FlightRoute r : flightRouteRepository.findByCompanyId(company.identity())) {
            if (r.isActiveOn(LocalDate.now())) {
                routes.add(r);
            }
        }
        return routes;
    }

    /**
     * Creates a flight plan for a given route.
     *
     * @param routeDesignation    e.g. "TP123"
     * @param aircraftRegistration e.g. "CS-TUA"
     * @param departureDateTime   must be in the future
     * @param fuelQuantity        must be strictly positive (kg)
     * @param companyIataHint     only required when the pilot has no company assignment;
     *                            pass null to derive the company from the pilot's assignment
     */
    public FlightPlan createFlightPlan(
            final String routeDesignation,
            final String aircraftRegistration,
            final Instant departureDateTime,
            final double fuelQuantity,
            final String companyIataHint) {

        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);

        if (departureDateTime == null || !departureDateTime.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Departure date/time must be in the future.");
        }
        if (fuelQuantity <= 0) {
            throw new IllegalArgumentException("Fuel quantity must be strictly positive.");
        }

        final String email = loggedInEmail();
        final UserId pilotUserId = UserId.valueOf(email);

        // Validate pilot
        final Pilot pilot = pilotRepository.findByUserId(pilotUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pilot profile found for user '" + email + "'."));
        if (pilot.status() != PilotStatus.ACTIVE) {
            throw new IllegalArgumentException("The assigned pilot is inactive and cannot be assigned to a flight plan.");
        }

        // Resolve company
        final AirTransportCompany company = resolveCompany(pilotUserId, companyIataHint);

        // Validate route
        final FlightRoute route = flightRouteRepository
                .findByDesignation(new RouteDesignation(routeDesignation))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Route '" + routeDesignation + "' not found."));
        if (!route.isActiveOn(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Route '" + routeDesignation + "' is inactive and cannot be used for new flight plans.");
        }
        if (!route.company().identity().equals(company.identity())) {
            throw new IllegalArgumentException(
                    "You are not authorized to create flight plans for this route.");
        }

        // Validate aircraft
        final Aircraft aircraft = aircraftRepository.findByRegistrationNumber(aircraftRegistration)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aircraft '" + aircraftRegistration + "' not found."));
        if (aircraft.operationStatus() == AircraftOperationStatus.DECOMISSIONED) {
            throw new IllegalArgumentException("Decommissioned aircraft cannot be assigned to a flight plan.");
        }
        if (!aircraft.airTransportCompany().identity().equals(company.identity())) {
            throw new IllegalArgumentException("The assigned aircraft must belong to the route's company.");
        }

        // Create plan — store essentials as JSON (full DSL required for complete plans)
        final String json = String.format(
                "{\"aircraftId\":\"%s\",\"pilotRef\":\"%s\",\"routeReference\":\"%s\"," +
                "\"departureDateTime\":\"%s\",\"fuelQuantity\":%.2f}",
                aircraft.registrationNumber(), email,
                route.identity().toString(), departureDateTime.toString(), fuelQuantity);
        final FlightPlan plan = new FlightPlan(FlightPlanId.newId(), json);

        return flightPlanRepository.save(plan);
    }

    private AirTransportCompany resolveCompany(final UserId pilotUserId, final String companyIataHint) {
        final Optional<PilotCompanyAssignment> assignment = assignmentRepository.findByPilotUserId(pilotUserId);
        if (assignment.isPresent()) {
            final AirTransportCompanyId companyId = assignment.get().companyId();
            return companyRepository.ofIdentity(companyId)
                    .orElseThrow(() -> new IllegalStateException("Company not found for assignment."));
        }

        if (companyIataHint == null || companyIataHint.isBlank()) {
            throw new IllegalArgumentException(
                    "Pilot has no company assignment. Please provide the company IATA code.");
        }
        final java.util.List<AirTransportCompany> matches =
                companyRepository.findByIataCode(companyIataHint.toUpperCase());
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Company with IATA code '" + companyIataHint + "' not found.");
        }
        return matches.get(0);
    }

    private String loggedInEmail() {
        return AuthzRegistry.authorizationService().session()
                .map(s -> s.authenticatedUser().identity().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated session."));
    }
}
