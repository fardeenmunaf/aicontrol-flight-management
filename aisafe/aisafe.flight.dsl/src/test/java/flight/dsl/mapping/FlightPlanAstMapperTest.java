package flight.dsl.mapping;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;
import flightPlan.domain.FlightLeg;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightType;
import node.domain.Node;
import node.repository.NodeRepository;
import org.junit.jupiter.api.Test;
import shared.domain.AirportId;
import shared.domain.NodeId;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanAstMapperTest {

    private static final String VALID_DSL = """
            flight TP1234 {
                type:      regular;
                aircraft:  A320;
                pilot:     atcc1@tap.pt;
                route:     AA123 from OPO to MAD;

                leg {
                    departure { airport: OPO; date: 2027-05-05; time: 08:00; }
                    arrival   { airport: MAD; date: 2027-05-05; time: 09:30; }
                    fuel { quantity: 40200 kg; }

                    profile {
                        climb   [ altitude: 69 m,   speed: 210 kts ];
                        climb   [ altitude: 1000 m, speed: 210 kts ];
                        climb   [ altitude: 2000 m, speed: 220 kts ];
                        climb   [ altitude: 3000 m, speed: 230 kts ];
                        climb   [ altitude: 4000 m, speed: 250 kts ];
                        climb   [ altitude: 5000 m, speed: 260 kts ];
                        climb   [ altitude: 6000 m, speed: 290 kts ];
                        climb   [ altitude: 7000 m, speed: 290 kts ];
                        climb   [ altitude: 8000 m, speed: 290 kts ];
                        climb   [ altitude: 9000 m, speed: 290 kts ];
                        climb   [ altitude: 9249 m, speed: 300 kts ];

                        cruise  [ speed: 460 kts ];

                        descend [ altitude: 9249 m, speed: 300 kts, rate: -10 m/s ];
                        descend [ altitude: 8000 m, speed: 300 kts, rate: -10 m/s ];
                        descend [ altitude: 7000 m, speed: 300 kts, rate: -10 m/s ];
                        descend [ altitude: 6000 m, speed: 300 kts, rate: -10 m/s ];
                        descend [ altitude: 5000 m, speed: 290 kts, rate: -10 m/s ];
                        descend [ altitude: 4000 m, speed: 270 kts, rate: -8 m/s  ];
                        descend [ altitude: 3000 m, speed: 250 kts, rate: -8 m/s  ];
                        descend [ altitude: 2000 m, speed: 250 kts, rate: -7 m/s  ];
                        descend [ altitude: 1000 m, speed: 180 kts, rate: -7 m/s  ];
                        descend [ altitude: 610 m,  speed: 140 kts, rate: -5 m/s  ];
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
            """;

    private FlightPlan mapValidPlan(final StubNodeRepository nodeRepo) {
        final FlightDslProcessingResult result = new FlightDslProcessor().process(VALID_DSL);
        assertTrue(result.isValid(), "test DSL must be valid: " + result.semanticErrors());
        return new FlightPlanAstMapper(new StubAirportRepository(), nodeRepo)
                .map(result.document().flightPlans().get(0));
    }

    @Test
    void ensureFlightLevelFieldsAreMapped() {
        final FlightPlan plan = mapValidPlan(new StubNodeRepository());
        assertAll(
                () -> assertEquals("TP1234", plan.flightDesignator()),
                () -> assertEquals(FlightType.REGULAR, plan.type()),
                () -> assertEquals("A320", plan.aircraftId()),
                () -> assertEquals("atcc1@tap.pt", plan.pilotRef()),
                () -> assertEquals("AA123", plan.routeReference()),
                () -> assertEquals("OPO", plan.routeOrigin()),
                () -> assertEquals("MAD", plan.routeDestination()),
                () -> assertEquals(Instant.parse("2027-05-05T08:00:00Z"), plan.departureTime()));
    }

    @Test
    void ensureLegSegmentsAndProfileAreMapped() {
        final FlightPlan plan = mapValidPlan(new StubNodeRepository());
        assertEquals(1, plan.legs().size());
        final FlightLeg leg = plan.legs().get(0);
        assertEquals(3, leg.segments().size());
        assertEquals(22, leg.profileEntries().size());
        assertEquals(1, leg.segments().get(0).altitudeSlots().size());
        assertEquals(270.0, leg.segments().get(0).windDirection());
    }

    @Test
    void ensureSharedJunctionNodesAreDeduplicated() {
        final StubNodeRepository nodeRepo = new StubNodeRepository();
        mapValidPlan(nodeRepo);
        assertEquals(4, nodeRepo.savedCount());
    }

    @Test
    void ensureUnknownAirportIsRejected() {
        final FlightDslProcessingResult result = new FlightDslProcessor().process(VALID_DSL);
        final FlightPlanAstMapper mapper = new FlightPlanAstMapper(new EmptyAirportRepository(), new StubNodeRepository());
        assertThrows(IllegalArgumentException.class, () -> mapper.map(result.document().flightPlans().get(0)));
    }

    // ── Stubs ────────────────────────────────────────────────────────────────
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

    static class EmptyAirportRepository
            extends InMemoryDomainRepository<Airport, AirportId> implements AirportRepository {
        @Override public Optional<Airport> findByIataCode(final String code) { return Optional.empty(); }
        @Override public Optional<Airport> findByIcaoCode(final String code) { return Optional.empty(); }
    }

    static class StubNodeRepository
            extends InMemoryDomainRepository<Node, NodeId> implements NodeRepository {
        private int saved = 0;
        int savedCount() { return saved; }
        @Override public Node save(final Node entity) { saved++; return super.save(entity); }
        @Override public Optional<Node> findByDesignation(final String d) { return Optional.empty(); }
    }
}
