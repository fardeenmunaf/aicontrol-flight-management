package flightPlan.application;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.repository.FlightPlanRepository;
import shared.domain.FlightPlanId;

import java.util.Objects;
import java.util.Optional;

public class ValidateFlightPlanController {

    private final FlightPlanRepository flightPlanRepository;

    public ValidateFlightPlanController(final FlightPlanRepository flightPlanRepository) {
        this.flightPlanRepository = Objects.requireNonNull(flightPlanRepository);
    }

    public Iterable<FlightPlan> draftPlansForLoggedInPilot() {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        final String pilotEmail = loggedInEmail();
        final Iterable<FlightPlan> all = flightPlanRepository.findByPilotRef(pilotEmail);
        final java.util.List<FlightPlan> drafts = new java.util.ArrayList<>();
        all.forEach(p -> {
            if (p.status() == FlightPlanStatus.DRAFT) drafts.add(p);
        });
        return drafts;
    }

    /**
     * Finds a flight plan by ID, verifying the logged-in pilot owns it and it is in DRAFT status.
     * Returns the plan ready for validation, or throws with a descriptive message.
     */
    public FlightPlan findForValidation(final FlightPlanId planId) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        final Optional<FlightPlan> opt = flightPlanRepository.findByFlightPlanId(planId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Flight plan not found: " + planId);
        }
        final FlightPlan plan = opt.get();
        if (!plan.pilotRef().equals(loggedInEmail())) {
            throw new SecurityException("You are not authorized to test this flight plan.");
        }
        if (plan.status() != FlightPlanStatus.DRAFT) {
            throw new IllegalStateException("Only flight plans in 'draft' status can be submitted for testing.");
        }
        return plan;
    }

    /**
     * Marks the flight plan as tested and persists the change.
     * Must only be called after all validation checks have passed.
     */
    public FlightPlan markAsTested(final FlightPlan plan) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        plan.markAsTested();
        return flightPlanRepository.save(plan);
    }

    private static String loggedInEmail() {
        return AuthzRegistry.authorizationService().session()
                .map(s -> s.authenticatedUser().identity().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated session"));
    }
}
