package flightPlan.application;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightPlan.domain.FlightPlan;
import flightPlan.repository.FlightPlanRepository;

import java.time.Duration;
import java.util.Objects;

public class CreateFlightPlanController {

    private static final Duration MIN_DEPARTURE_INTERVAL = Duration.ofHours(1);
    private static final Duration PILOT_REST_PERIOD = Duration.ofHours(1);

    private final FlightPlanRepository flightPlanRepository;

    public CreateFlightPlanController(final FlightPlanRepository flightPlanRepository) {
        this.flightPlanRepository = Objects.requireNonNull(flightPlanRepository);
    }

    /**
     * Persists a validated, fully-built flight plan aggregate.
     * The aggregate is produced by the DSL→aggregate mapper (Plan 2) from a DSL-validated AST.
     * Rejects the plan if another plan with the same designator departs within 1 hour,
     * or if the assigned pilot has not completed the required rest period.
     */
    public FlightPlan create(final FlightPlan plan) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(
                AISafeRoles.PILOT, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        if (plan == null) {
            throw new IllegalArgumentException("Flight plan must not be null");
        }
        checkMinDepartureInterval(plan);
        checkPilotRestPeriod(plan);
        return flightPlanRepository.save(plan);
    }

    private void checkMinDepartureInterval(final FlightPlan newPlan) {
        final java.time.Instant newDep = newPlan.departureTime();
        for (final FlightPlan existing : flightPlanRepository.findByFlightDesignator(newPlan.flightDesignator())) {
            final java.time.Instant exDep = existing.departureTime();
            final Duration gap = Duration.between(exDep, newDep).abs();
            if (gap.compareTo(MIN_DEPARTURE_INTERVAL) < 0) {
                throw new IllegalArgumentException(
                        "Flight " + newPlan.flightDesignator() + " already has a plan departing at "
                                + exDep + ". Flights of the same designator must depart at least "
                                + MIN_DEPARTURE_INTERVAL.toHours() + "h apart.");
            }
        }
    }

    private void checkPilotRestPeriod(final FlightPlan newPlan) {
        final java.time.Instant newDep = newPlan.departureTime();
        final java.time.Instant newArr = newPlan.arrivalTime();
        for (final FlightPlan existing : flightPlanRepository.findByPilotRef(newPlan.pilotRef())) {
            final java.time.Instant exDep = existing.departureTime();
            final java.time.Instant exArr = existing.arrivalTime();
            if (newDep.isBefore(exArr.plus(PILOT_REST_PERIOD)) && newArr.plus(PILOT_REST_PERIOD).isAfter(exDep)) {
                throw new IllegalArgumentException(
                        "Pilot " + newPlan.pilotRef() + " requires " + PILOT_REST_PERIOD.toHours()
                                + "h rest after landing. Conflicting flight operates between "
                                + exDep + " and " + exArr + ".");
            }
        }
    }
}
