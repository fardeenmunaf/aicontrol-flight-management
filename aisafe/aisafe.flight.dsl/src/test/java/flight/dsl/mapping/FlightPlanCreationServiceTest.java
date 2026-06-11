package flight.dsl.mapping;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
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
import shared.domain.AirportId;
import shared.domain.FlightPlanId;
import shared.domain.NodeId;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanCreationServiceTest {

    private static final String PILOT = "svc.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private final StubFlightPlanRepository planRepo = new StubFlightPlanRepository();

    @BeforeAll
    static void auth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (final Exception ignored) { }
        try {
            AuthzRegistry.userService().registerNewUser(PILOT, PASSWORD, "P", "P", PILOT, Set.of(AISafeRoles.PILOT));
        } catch (final Exception ignored) { }
    }

    @BeforeEach
    void login() {
        planRepo.clear();
        AuthzRegistry.authenticationService().authenticate(PILOT, PASSWORD);
    }

    private FlightPlanCreationService service() {
        return new FlightPlanCreationService(planRepo, new StubAirportRepository(), new StubNodeRepository());
    }

    @Test
    void ensureValidDslCreatesAndPersistsPlan() {
        final CreationResult result = service().createFromText(validDsl());
        assertFalse(result.hasErrors(), result.errors().toString());
        assertEquals(1, result.created().size());
        assertEquals(FlightPlanStatus.DRAFT, result.created().get(0).status());
        assertEquals(1, planRepo.count());
    }

    @Test
    void ensureInvalidDslProducesErrorsAndNoPlans() {
        final CreationResult result = service().createFromText("flight !!! broken {");
        assertTrue(result.hasErrors());
        assertEquals(0, result.created().size());
        assertEquals(0, planRepo.count());
    }

    private static String validDsl() {
        return """
                flight TP1234 {
                    type:      regular;
                    aircraft:  A320;
                    pilot:     %s;
                    route:     AA123 from OPO to MAD;
                    leg {
                        departure { airport: OPO; date: 2027-05-05; time: 08:00; }
                        arrival   { airport: MAD; date: 2027-05-05; time: 09:30; }
                        fuel { quantity: 40200 kg; }
                        profile {
                            climb   [ altitude: 69 m,   speed: 210 kts ];
                            climb   [ altitude: 1000 m, speed: 210 kts ];
                            climb   [ altitude: 9249 m, speed: 300 kts ];
                            cruise  [ speed: 460 kts ];
                            descend [ altitude: 9249 m, speed: 300 kts, rate: -10 m/s ];
                            descend [ altitude: 1000 m, speed: 180 kts, rate: -7 m/s ];
                            descend [ altitude: 610 m,  speed: 140 kts, rate: -5 m/s ];
                        }
                        segment {
                            from: (41.262891, -8.68522, altitude: 69 m);
                            to:   (42.0, -8.01, altitude: 9249 m);
                            altslot [ min: 0 m, max: 9249 m, width: 50 m ];
                            wind { direction: 270; speed: 15 m/s; }
                            mode: climb;
                        }
                        segment {
                            from: (42.0, -8.01, altitude: 9249 m);
                            to:   (41.0, -4.3, altitude: 9249 m);
                            altslot [ min: 9000 m, max: 9500 m, width: 80 m ];
                            wind { direction: 260; speed: 20 m/s; }
                            mode: cruise;
                        }
                        segment {
                            from: (41.0, -4.3, altitude: 9249 m);
                            to:   (40.4895, -3.5643, altitude: 610 m);
                            altslot [ min: 0 m, max: 9249 m, width: 50 m ];
                            wind { direction: 250; speed: 10 m/s; }
                            mode: descend;
                        }
                    }
                }
                """.formatted(PILOT);
    }

    static class DummyAirport extends Airport {
        private final AirportId id = AirportId.newId();
        DummyAirport() { super(); }
        @Override public AirportId identity() { return id; }
    }

    static class StubAirportRepository
            extends InMemoryDomainRepository<Airport, AirportId> implements AirportRepository {
        @Override public Optional<Airport> findByIataCode(final String code) { return Optional.of(new DummyAirport()); }
        @Override public Optional<Airport> findByIcaoCode(final String code) { return Optional.empty(); }
    }

    static class StubNodeRepository
            extends InMemoryDomainRepository<Node, NodeId> implements NodeRepository {
        @Override public Optional<Node> findByDesignation(final String d) { return Optional.empty(); }
    }

    static class StubFlightPlanRepository
            extends InMemoryDomainRepository<FlightPlan, FlightPlanId> implements FlightPlanRepository {
        void clear() { data().clear(); }
        @Override public Optional<FlightPlan> findByFlightPlanId(final FlightPlanId id) {
            return matchOne(p -> p.identity().equals(id));
        }
        @Override public Iterable<FlightPlan> findByStatus(final FlightPlanStatus s) { return match(p -> p.status() == s); }
        @Override public Iterable<FlightPlan> findByRouteIdAndDepartureAfter(final String routeId, final Instant departure) {
            return match(p -> routeId.equals(p.routeReference())
                    && p.departureTime() != null && p.departureTime().isAfter(departure));
        }
    }
}
