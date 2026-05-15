package flightPlan.application;

import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.repository.FlightPlanRepository;
import node.domain.Node;
import node.repository.NodeRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.FlightPlanId;
import shared.domain.NodeId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImportFlightPlanControllerTest {

    private static final String PILOT_EMAIL = "import.pilot@test.com";
    private static final String PASSWORD    = "Password123";

    private final StubFlightPlanRepository planRepo = new StubFlightPlanRepository();
    private final StubNodeRepository       nodeRepo = new StubNodeRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (final Exception ignored) {}
        try {
            AuthzRegistry.userService().registerNewUser(PILOT_EMAIL, PASSWORD, "P", "P", PILOT_EMAIL, Set.of(AISafeRoles.PILOT));
        } catch (final Exception ignored) {}
    }

    @BeforeEach
    void setup() {
        planRepo.clear();
        nodeRepo.clear();
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
    }

    private ImportFlightPlanController controller() {
        return new ImportFlightPlanController(planRepo, nodeRepo);
    }

    private LegImportRequest singleLeg() {
        return new LegImportRequest(1,
                "2026-05-05", "08:00", "2026-05-05", "09:30",
                40200.0, "OPO", "MAD",
                List.of(new SegmentImportRequest(41.26, -8.68, 40.49, -3.56)));
    }

    @Test
    void ensureFlightPlanIsImportedSuccessfully() {
        final FlightPlan plan = controller().importFlightPlan("TP1234", List.of(singleLeg()));
        assertNotNull(plan);
    }

    @Test
    void ensureNullDesignatorIsRejected() {
        assertThrows(NullPointerException.class, () ->
                controller().importFlightPlan(null, List.of(singleLeg())));
    }

    @Test
    void ensureEmptyLegsListIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().importFlightPlan("TP1234", List.of()));
    }

    @Test
    void ensureNullLegsIsRejected() {
        assertThrows(Exception.class, () ->
                controller().importFlightPlan("TP1234", null));
    }

    @Test
    void ensureDuplicateDesignatorIsRejected() {
        controller().importFlightPlan("TP-DUPE", List.of(singleLeg()));
        assertThrows(IllegalArgumentException.class, () ->
                controller().importFlightPlan("TP-DUPE", List.of(singleLeg())));
    }

    @Test
    void ensureMultipleLegsAreImported() {
        final LegImportRequest leg2 = new LegImportRequest(2,
                "2026-05-05", "11:00", "2026-05-05", "12:30",
                38000.0, "MAD", "BCN",
                List.of(new SegmentImportRequest(40.49, -3.56, 41.29, 2.07)));
        final FlightPlan plan = controller().importFlightPlan("TP5678", List.of(singleLeg(), leg2));
        assertNotNull(plan);
    }

    static class StubFlightPlanRepository
            extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
            implements FlightPlanRepository {

        void clear() { data().clear(); }

        @Override
        public Optional<FlightPlan> findByFlightPlanId(final FlightPlanId id) {
            return matchOne(p -> p.identity().equals(id));
        }

        @Override public Iterable<FlightPlan> findByStatus(final FlightPlanStatus s) { return List.of(); }
        @Override public Iterable<FlightPlan> findByDepartureBetween(final Instant a, final Instant b) { return List.of(); }
    }

    static class StubNodeRepository
            extends InMemoryDomainRepository<Node, NodeId>
            implements NodeRepository {

        void clear() { data().clear(); }

        @Override public Optional<Node> findByDesignation(final String d) { return Optional.empty(); }
    }
}
