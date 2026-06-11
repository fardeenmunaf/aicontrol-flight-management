package flightPlan.application;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightPlan.domain.FlightPlan;
import flightPlan.repository.FlightPlanRepository;

import java.util.Objects;

public class CreateFlightPlanController {

    private final FlightPlanRepository flightPlanRepository;

    public CreateFlightPlanController(final FlightPlanRepository flightPlanRepository) {
        this.flightPlanRepository = Objects.requireNonNull(flightPlanRepository);
    }

    /**
     * Persists a validated, fully-built flight plan aggregate.
     * The aggregate is produced by the DSL→aggregate mapper (Plan 2) from a DSL-validated AST.
     */
    public FlightPlan create(final FlightPlan plan) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(
                AISafeRoles.PILOT, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        if (plan == null) {
            throw new IllegalArgumentException("Flight plan must not be null");
        }
        return flightPlanRepository.save(plan);
    }
}
