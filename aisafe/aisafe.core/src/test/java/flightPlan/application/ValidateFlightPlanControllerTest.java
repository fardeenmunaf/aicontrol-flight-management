package flightPlan.application;

import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flightPlan.domain.FlightLeg;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.domain.FlightType;
import flightPlan.repository.FlightPlanRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AirportId;
import shared.domain.FlightPlanId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateFlightPlanControllerTest {

    private static final String PILOT_EMAIL = "validate.pilot@test.com";
    private static final String OTHER_PILOT = "other.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private final StubRepo repo = new StubRepo();

    @BeforeAll
    static void auth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (final Exception ignored) { }
        try {
            AuthzRegistry.userService().registerNewUser(PILOT_EMAIL, PASSWORD, "V", "P", PILOT_EMAIL,
                    Set.of(AISafeRoles.PILOT));
        } catch (final Exception ignored) { }
        try {
            AuthzRegistry.userService().registerNewUser(OTHER_PILOT, PASSWORD, "O", "P", OTHER_PILOT,
                    Set.of(AISafeRoles.PILOT));
        } catch (final Exception ignored) { }
    }

    @BeforeEach
    void setup() {
        repo.clear();
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
    }

    private FlightPlan draftPlan(final String pilotRef) {
        final FlightLeg leg = new FlightLeg(1,
                Instant.parse("2027-06-01T08:00:00Z"), Instant.parse("2027-06-01T10:00:00Z"),
                4000.0, AirportId.valueOf("apt-LIS"), AirportId.valueOf("apt-MAD"));
        return new FlightPlan(FlightPlanId.newId(), "TP9900", FlightType.REGULAR,
                "A320", pilotRef, "RT001", "LIS", "MAD", List.of(leg));
    }

    @Test
    void ensureDraftPlansForLoggedInPilotReturnsOwnPlans() {
        final FlightPlan own = draftPlan(PILOT_EMAIL);
        final FlightPlan other = draftPlan(OTHER_PILOT);
        repo.save(own);
        repo.save(other);

        final ValidateFlightPlanController ctrl = new ValidateFlightPlanController(repo);
        final List<FlightPlan> drafts = new java.util.ArrayList<>();
        ctrl.draftPlansForLoggedInPilot().forEach(drafts::add);

        assertEquals(1, drafts.size());
        assertEquals(PILOT_EMAIL, drafts.get(0).pilotRef());
    }

    @Test
    void ensureFindForValidationRejectsNonDraftPlan() {
        final FlightPlan plan = draftPlan(PILOT_EMAIL);
        plan.markAsTested();
        repo.save(plan);

        final ValidateFlightPlanController ctrl = new ValidateFlightPlanController(repo);
        assertThrows(IllegalStateException.class, () -> ctrl.findForValidation(plan.identity()));
    }

    @Test
    void ensureFindForValidationRejectsOtherPilotPlan() {
        final FlightPlan plan = draftPlan(OTHER_PILOT);
        repo.save(plan);

        final ValidateFlightPlanController ctrl = new ValidateFlightPlanController(repo);
        assertThrows(SecurityException.class, () -> ctrl.findForValidation(plan.identity()));
    }

    @Test
    void ensureFindForValidationReturnsOwnDraftPlan() {
        final FlightPlan plan = draftPlan(PILOT_EMAIL);
        repo.save(plan);

        final ValidateFlightPlanController ctrl = new ValidateFlightPlanController(repo);
        final FlightPlan found = ctrl.findForValidation(plan.identity());
        assertNotNull(found);
        assertEquals(FlightPlanStatus.DRAFT, found.status());
    }

    @Test
    void ensureMarkAsTestedUpdatesPlanStatus() {
        final FlightPlan plan = draftPlan(PILOT_EMAIL);
        repo.save(plan);

        final ValidateFlightPlanController ctrl = new ValidateFlightPlanController(repo);
        ctrl.markAsTested(plan);

        final Optional<FlightPlan> saved = repo.findByFlightPlanId(plan.identity());
        assertTrue(saved.isPresent());
        assertEquals(FlightPlanStatus.TESTED, saved.get().status());
    }

    static class StubRepo extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
            implements FlightPlanRepository {
        void clear() { data().clear(); }
        @Override public Optional<FlightPlan> findByFlightPlanId(final FlightPlanId id) {
            return matchOne(p -> p.identity().equals(id));
        }
        @Override public Iterable<FlightPlan> findByStatus(final FlightPlanStatus s) {
            return match(p -> p.status() == s);
        }
        @Override public Iterable<FlightPlan> findByRouteIdAndDepartureAfter(final String r, final Instant d) {
            return match(p -> r.equals(p.routeReference()) && p.departureTime() != null && p.departureTime().isAfter(d));
        }
        @Override public Iterable<FlightPlan> findByPilotRef(final String pilotRef) {
            return match(p -> pilotRef.equals(p.pilotRef()));
        }
    }
}
