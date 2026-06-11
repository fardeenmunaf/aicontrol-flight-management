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

class CreateFlightPlanControllerTest {

    private static final String PILOT_EMAIL = "create.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private final StubFlightPlanRepository planRepo = new StubFlightPlanRepository();

    @BeforeAll
    static void auth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (final Exception ignored) { }
        try {
            AuthzRegistry.userService().registerNewUser(PILOT_EMAIL, PASSWORD, "P", "P", PILOT_EMAIL,
                    Set.of(AISafeRoles.PILOT));
        } catch (final Exception ignored) { }
    }

    @BeforeEach
    void login() {
        planRepo.clear();
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
    }

    private FlightPlan plan() {
        final FlightLeg leg = new FlightLeg(1,
                Instant.parse("2027-05-05T08:00:00Z"), Instant.parse("2027-05-05T09:30:00Z"),
                4200.0, AirportId.valueOf("apt-OPO"), AirportId.valueOf("apt-MAD"));
        return new FlightPlan(FlightPlanId.newId(), "TP1234", FlightType.REGULAR,
                "A320", PILOT_EMAIL, "AA123", "OPO", "MAD", List.of(leg));
    }

    @Test
    void ensurePlanIsPersistedAsDraft() {
        final FlightPlan saved = new CreateFlightPlanController(planRepo).create(plan());
        assertNotNull(saved);
        assertEquals(FlightPlanStatus.DRAFT, saved.status());
        assertTrue(planRepo.findByFlightPlanId(saved.identity()).isPresent());
    }

    @Test
    void ensureNullPlanIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CreateFlightPlanController(planRepo).create(null));
    }

    static class StubFlightPlanRepository
            extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
            implements FlightPlanRepository {
        void clear() { data().clear(); }
        @Override public Optional<FlightPlan> findByFlightPlanId(final FlightPlanId id) {
            return matchOne(p -> p.identity().equals(id));
        }
        @Override public Iterable<FlightPlan> findByStatus(final FlightPlanStatus s) {
            return match(p -> p.status() == s);
        }
        @Override public Iterable<FlightPlan> findByRouteIdAndDepartureAfter(final String routeId, final Instant departure) {
            return match(p -> routeId.equals(p.routeReference())
                    && p.departureTime() != null && p.departureTime().isAfter(departure));
        }
    }
}
