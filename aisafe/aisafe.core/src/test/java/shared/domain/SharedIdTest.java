package shared.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all *Id classes in shared.domain to ensure ≥90% method coverage.
 * Each Id class has: valueOf, newId, compareTo, toString, equals, hashCode.
 */
class SharedIdTest {

    // ---- AircraftId ----

    @Test
    void ensureAircraftIdValueOfCreatesId() {
        final AircraftId id = AircraftId.valueOf("AC-001");
        assertEquals("AC-001", id.toString());
    }

    @Test
    void ensureAircraftIdNewIdIsUnique() {
        final AircraftId id1 = AircraftId.newId();
        final AircraftId id2 = AircraftId.newId();
        assertNotEquals(id1, id2);
    }

    @Test
    void ensureAircraftIdEqualsSameValue() {
        final AircraftId id1 = AircraftId.valueOf("AC-SAME");
        final AircraftId id2 = AircraftId.valueOf("AC-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureAircraftIdNotEqualsNull() {
        assertNotEquals(AircraftId.valueOf("AC-001"), null);
    }

    @Test
    void ensureAircraftIdNotEqualsDifferent() {
        assertNotEquals(AircraftId.valueOf("AC-001"), AircraftId.valueOf("AC-002"));
    }

    @Test
    void ensureAircraftIdCompareToSameIsZero() {
        final AircraftId id = AircraftId.valueOf("AC-A");
        assertEquals(0, id.compareTo(AircraftId.valueOf("AC-A")));
    }

    @Test
    void ensureAircraftIdCompareToSmaller() {
        assertTrue(AircraftId.valueOf("AC-A").compareTo(AircraftId.valueOf("AC-B")) < 0);
    }

    // ---- AircraftModelId ----

    @Test
    void ensureAircraftModelIdValueOfCreatesId() {
        final AircraftModelId id = AircraftModelId.valueOf("AM-001");
        assertEquals("AM-001", id.toString());
    }

    @Test
    void ensureAircraftModelIdNewIdIsUnique() {
        assertNotEquals(AircraftModelId.newId(), AircraftModelId.newId());
    }

    @Test
    void ensureAircraftModelIdEqualsSameValue() {
        final AircraftModelId id1 = AircraftModelId.valueOf("AM-SAME");
        final AircraftModelId id2 = AircraftModelId.valueOf("AM-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureAircraftModelIdNotEqualsNull() {
        assertNotEquals(AircraftModelId.valueOf("AM-001"), null);
    }

    @Test
    void ensureAircraftModelIdCompareTo() {
        assertEquals(0, AircraftModelId.valueOf("AM-A").compareTo(AircraftModelId.valueOf("AM-A")));
        assertTrue(AircraftModelId.valueOf("AM-A").compareTo(AircraftModelId.valueOf("AM-B")) < 0);
    }

    // ---- AirControlAreaId ----

    @Test
    void ensureAirControlAreaIdValueOfCreatesId() {
        final AirControlAreaId id = AirControlAreaId.valueOf("AREA-001");
        assertEquals("AREA-001", id.toString());
    }

    @Test
    void ensureAirControlAreaIdNewIdIsUnique() {
        assertNotEquals(AirControlAreaId.newId(), AirControlAreaId.newId());
    }

    @Test
    void ensureAirControlAreaIdEqualsSameValue() {
        final AirControlAreaId id1 = AirControlAreaId.valueOf("AREA-SAME");
        final AirControlAreaId id2 = AirControlAreaId.valueOf("AREA-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureAirControlAreaIdNotEqualsNull() {
        assertNotEquals(AirControlAreaId.valueOf("AREA-001"), null);
    }

    @Test
    void ensureAirControlAreaIdCompareTo() {
        assertEquals(0, AirControlAreaId.valueOf("AREA-A").compareTo(AirControlAreaId.valueOf("AREA-A")));
        assertTrue(AirControlAreaId.valueOf("AREA-A").compareTo(AirControlAreaId.valueOf("AREA-B")) < 0);
    }

    // ---- AirportId ----

    @Test
    void ensureAirportIdValueOfCreatesId() {
        final AirportId id = AirportId.valueOf("LIS");
        assertEquals("LIS", id.toString());
    }

    @Test
    void ensureAirportIdNewIdIsUnique() {
        assertNotEquals(AirportId.newId(), AirportId.newId());
    }

    @Test
    void ensureAirportIdEqualsSameValue() {
        final AirportId id1 = AirportId.valueOf("LIS");
        final AirportId id2 = AirportId.valueOf("LIS");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureAirportIdNotEqualsNull() {
        assertNotEquals(AirportId.valueOf("LIS"), null);
    }

    @Test
    void ensureAirportIdNotEqualsDifferent() {
        assertNotEquals(AirportId.valueOf("LIS"), AirportId.valueOf("OPO"));
    }

    @Test
    void ensureAirportIdCompareTo() {
        assertEquals(0, AirportId.valueOf("LIS").compareTo(AirportId.valueOf("LIS")));
        assertTrue(AirportId.valueOf("LIS").compareTo(AirportId.valueOf("OPO")) < 0);
    }

    // ---- AirTransportCompanyId ----

    @Test
    void ensureAirTransportCompanyIdValueOfCreatesId() {
        final AirTransportCompanyId id = AirTransportCompanyId.valueOf("ATC-001");
        assertEquals("ATC-001", id.toString());
    }

    @Test
    void ensureAirTransportCompanyIdNewIdIsUnique() {
        assertNotEquals(AirTransportCompanyId.newId(), AirTransportCompanyId.newId());
    }

    @Test
    void ensureAirTransportCompanyIdEqualsSameValue() {
        final AirTransportCompanyId id1 = AirTransportCompanyId.valueOf("ATC-SAME");
        final AirTransportCompanyId id2 = AirTransportCompanyId.valueOf("ATC-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureAirTransportCompanyIdNotEqualsNull() {
        assertNotEquals(AirTransportCompanyId.valueOf("ATC-001"), null);
    }

    @Test
    void ensureAirTransportCompanyIdCompareTo() {
        assertEquals(0, AirTransportCompanyId.valueOf("ATC-A").compareTo(AirTransportCompanyId.valueOf("ATC-A")));
        assertTrue(AirTransportCompanyId.valueOf("ATC-A").compareTo(AirTransportCompanyId.valueOf("ATC-B")) < 0);
    }

    // ---- CustomerCollaboratorId ----

    @Test
    void ensureCustomerCollaboratorIdValueOfCreatesId() {
        final CustomerCollaboratorId id = CustomerCollaboratorId.valueOf("CC-001");
        assertEquals("CC-001", id.toString());
    }

    @Test
    void ensureCustomerCollaboratorIdNewIdIsUnique() {
        assertNotEquals(CustomerCollaboratorId.newId(), CustomerCollaboratorId.newId());
    }

    @Test
    void ensureCustomerCollaboratorIdEqualsSameValue() {
        final CustomerCollaboratorId id1 = CustomerCollaboratorId.valueOf("CC-SAME");
        final CustomerCollaboratorId id2 = CustomerCollaboratorId.valueOf("CC-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureCustomerCollaboratorIdNotEqualsNull() {
        assertNotEquals(CustomerCollaboratorId.valueOf("CC-001"), null);
    }

    @Test
    void ensureCustomerCollaboratorIdNotEqualsDifferent() {
        assertNotEquals(CustomerCollaboratorId.valueOf("CC-001"), CustomerCollaboratorId.valueOf("CC-002"));
    }

    @Test
    void ensureCustomerCollaboratorIdCompareTo() {
        assertEquals(0, CustomerCollaboratorId.valueOf("CC-A").compareTo(CustomerCollaboratorId.valueOf("CC-A")));
        assertTrue(CustomerCollaboratorId.valueOf("CC-A").compareTo(CustomerCollaboratorId.valueOf("CC-B")) < 0);
    }

    // ---- EngineModelId ----

    @Test
    void ensureEngineModelIdValueOfCreatesId() {
        final EngineModelId id = EngineModelId.valueOf("EM-001");
        assertEquals("EM-001", id.toString());
    }

    @Test
    void ensureEngineModelIdNewIdIsUnique() {
        assertNotEquals(EngineModelId.newId(), EngineModelId.newId());
    }

    @Test
    void ensureEngineModelIdEqualsSameValue() {
        final EngineModelId id1 = EngineModelId.valueOf("EM-SAME");
        final EngineModelId id2 = EngineModelId.valueOf("EM-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureEngineModelIdNotEqualsNull() {
        assertNotEquals(EngineModelId.valueOf("EM-001"), null);
    }

    @Test
    void ensureEngineModelIdCompareTo() {
        assertEquals(0, EngineModelId.valueOf("EM-A").compareTo(EngineModelId.valueOf("EM-A")));
        assertTrue(EngineModelId.valueOf("EM-A").compareTo(EngineModelId.valueOf("EM-B")) < 0);
    }

    // ---- FlightId ----

    @Test
    void ensureFlightIdValueOfCreatesId() {
        final FlightId id = FlightId.valueOf("FL-001");
        assertEquals("FL-001", id.toString());
    }

    @Test
    void ensureFlightIdNewIdIsUnique() {
        assertNotEquals(FlightId.newId(), FlightId.newId());
    }

    @Test
    void ensureFlightIdEqualsSameValue() {
        final FlightId id1 = FlightId.valueOf("FL-SAME");
        final FlightId id2 = FlightId.valueOf("FL-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureFlightIdNotEqualsNull() {
        assertNotEquals(FlightId.valueOf("FL-001"), null);
    }

    @Test
    void ensureFlightIdCompareTo() {
        assertEquals(0, FlightId.valueOf("FL-A").compareTo(FlightId.valueOf("FL-A")));
        assertTrue(FlightId.valueOf("FL-A").compareTo(FlightId.valueOf("FL-B")) < 0);
    }

    // ---- FlightPlanId ----

    @Test
    void ensureFlightPlanIdValueOfCreatesId() {
        final FlightPlanId id = FlightPlanId.valueOf("FP-001");
        assertEquals("FP-001", id.toString());
    }

    @Test
    void ensureFlightPlanIdNewIdIsUnique() {
        assertNotEquals(FlightPlanId.newId(), FlightPlanId.newId());
    }

    @Test
    void ensureFlightPlanIdEqualsSameValue() {
        final FlightPlanId id1 = FlightPlanId.valueOf("FP-SAME");
        final FlightPlanId id2 = FlightPlanId.valueOf("FP-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureFlightPlanIdNotEqualsNull() {
        assertNotEquals(FlightPlanId.valueOf("FP-001"), null);
    }

    @Test
    void ensureFlightPlanIdCompareTo() {
        assertEquals(0, FlightPlanId.valueOf("FP-A").compareTo(FlightPlanId.valueOf("FP-A")));
        assertTrue(FlightPlanId.valueOf("FP-A").compareTo(FlightPlanId.valueOf("FP-B")) < 0);
    }

    // ---- FlightRouteId ----

    @Test
    void ensureFlightRouteIdValueOfCreatesId() {
        final FlightRouteId id = FlightRouteId.valueOf("FR-001");
        assertEquals("FR-001", id.toString());
    }

    @Test
    void ensureFlightRouteIdNewIdIsUnique() {
        assertNotEquals(FlightRouteId.newId(), FlightRouteId.newId());
    }

    @Test
    void ensureFlightRouteIdEqualsSameValue() {
        final FlightRouteId id1 = FlightRouteId.valueOf("FR-SAME");
        final FlightRouteId id2 = FlightRouteId.valueOf("FR-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureFlightRouteIdNotEqualsNull() {
        assertNotEquals(FlightRouteId.valueOf("FR-001"), null);
    }

    @Test
    void ensureFlightRouteIdCompareTo() {
        assertEquals(0, FlightRouteId.valueOf("FR-A").compareTo(FlightRouteId.valueOf("FR-A")));
        assertTrue(FlightRouteId.valueOf("FR-A").compareTo(FlightRouteId.valueOf("FR-B")) < 0);
    }

    // ---- NodeId ----

    @Test
    void ensureNodeIdValueOfCreatesId() {
        final NodeId id = NodeId.valueOf("ND-001");
        assertEquals("ND-001", id.toString());
    }

    @Test
    void ensureNodeIdNewIdIsUnique() {
        assertNotEquals(NodeId.newId(), NodeId.newId());
    }

    @Test
    void ensureNodeIdEqualsSameValue() {
        final NodeId id1 = NodeId.valueOf("ND-SAME");
        final NodeId id2 = NodeId.valueOf("ND-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureNodeIdNotEqualsNull() {
        assertNotEquals(NodeId.valueOf("ND-001"), null);
    }

    @Test
    void ensureNodeIdNotEqualsDifferent() {
        assertNotEquals(NodeId.valueOf("ND-001"), NodeId.valueOf("ND-002"));
    }

    @Test
    void ensureNodeIdCompareTo() {
        assertEquals(0, NodeId.valueOf("ND-A").compareTo(NodeId.valueOf("ND-A")));
        assertTrue(NodeId.valueOf("ND-A").compareTo(NodeId.valueOf("ND-B")) < 0);
    }

    // ---- PilotId ----

    @Test
    void ensurePilotIdValueOfCreatesId() {
        final PilotId id = PilotId.valueOf("PI-001");
        assertEquals("PI-001", id.toString());
    }

    @Test
    void ensurePilotIdNewIdIsUnique() {
        assertNotEquals(PilotId.newId(), PilotId.newId());
    }

    @Test
    void ensurePilotIdEqualsSameValue() {
        final PilotId id1 = PilotId.valueOf("PI-SAME");
        final PilotId id2 = PilotId.valueOf("PI-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensurePilotIdNotEqualsNull() {
        assertNotEquals(PilotId.valueOf("PI-001"), null);
    }

    @Test
    void ensurePilotIdCompareTo() {
        assertEquals(0, PilotId.valueOf("PI-A").compareTo(PilotId.valueOf("PI-A")));
        assertTrue(PilotId.valueOf("PI-A").compareTo(PilotId.valueOf("PI-B")) < 0);
    }

    // ---- SimulationId ----

    @Test
    void ensureSimulationIdValueOfCreatesId() {
        final SimulationId id = SimulationId.valueOf("SIM-001");
        assertEquals("SIM-001", id.toString());
    }

    @Test
    void ensureSimulationIdNewIdIsUnique() {
        assertNotEquals(SimulationId.newId(), SimulationId.newId());
    }

    @Test
    void ensureSimulationIdEqualsSameValue() {
        final SimulationId id1 = SimulationId.valueOf("SIM-SAME");
        final SimulationId id2 = SimulationId.valueOf("SIM-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureSimulationIdNotEqualsNull() {
        assertNotEquals(SimulationId.valueOf("SIM-001"), null);
    }

    @Test
    void ensureSimulationIdCompareTo() {
        assertEquals(0, SimulationId.valueOf("SIM-A").compareTo(SimulationId.valueOf("SIM-A")));
        assertTrue(SimulationId.valueOf("SIM-A").compareTo(SimulationId.valueOf("SIM-B")) < 0);
    }

    // ---- SimulationReportId ----

    @Test
    void ensureSimulationReportIdValueOfCreatesId() {
        final SimulationReportId id = SimulationReportId.valueOf("SR-001");
        assertEquals("SR-001", id.toString());
    }

    @Test
    void ensureSimulationReportIdNewIdIsUnique() {
        assertNotEquals(SimulationReportId.newId(), SimulationReportId.newId());
    }

    @Test
    void ensureSimulationReportIdEqualsSameValue() {
        final SimulationReportId id1 = SimulationReportId.valueOf("SR-SAME");
        final SimulationReportId id2 = SimulationReportId.valueOf("SR-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureSimulationReportIdNotEqualsNull() {
        assertNotEquals(SimulationReportId.valueOf("SR-001"), null);
    }

    @Test
    void ensureSimulationReportIdCompareTo() {
        assertEquals(0, SimulationReportId.valueOf("SR-A").compareTo(SimulationReportId.valueOf("SR-A")));
        assertTrue(SimulationReportId.valueOf("SR-A").compareTo(SimulationReportId.valueOf("SR-B")) < 0);
    }

    // ---- UserId ----

    @Test
    void ensureUserIdValueOfCreatesId() {
        final UserId id = UserId.valueOf("user@test.com");
        assertEquals("user@test.com", id.toString());
    }

    @Test
    void ensureUserIdNewIdIsUnique() {
        assertNotEquals(UserId.newId(), UserId.newId());
    }

    @Test
    void ensureUserIdEqualsSameValue() {
        final UserId id1 = UserId.valueOf("user@test.com");
        final UserId id2 = UserId.valueOf("user@test.com");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureUserIdNotEqualsNull() {
        assertNotEquals(UserId.valueOf("user@test.com"), null);
    }

    @Test
    void ensureUserIdNotEqualsDifferent() {
        assertNotEquals(UserId.valueOf("user1@test.com"), UserId.valueOf("user2@test.com"));
    }

    @Test
    void ensureUserIdCompareTo() {
        assertEquals(0, UserId.valueOf("user@test.com").compareTo(UserId.valueOf("user@test.com")));
        assertTrue(UserId.valueOf("a@test.com").compareTo(UserId.valueOf("b@test.com")) < 0);
    }

    // ---- WeatherDataId ----

    @Test
    void ensureWeatherDataIdValueOfCreatesId() {
        final WeatherDataId id = WeatherDataId.valueOf("WD-001");
        assertEquals("WD-001", id.toString());
    }

    @Test
    void ensureWeatherDataIdNewIdIsUnique() {
        assertNotEquals(WeatherDataId.newId(), WeatherDataId.newId());
    }

    @Test
    void ensureWeatherDataIdEqualsSameValue() {
        final WeatherDataId id1 = WeatherDataId.valueOf("WD-SAME");
        final WeatherDataId id2 = WeatherDataId.valueOf("WD-SAME");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void ensureWeatherDataIdNotEqualsNull() {
        assertNotEquals(WeatherDataId.valueOf("WD-001"), null);
    }

    @Test
    void ensureWeatherDataIdCompareTo() {
        assertEquals(0, WeatherDataId.valueOf("WD-A").compareTo(WeatherDataId.valueOf("WD-A")));
        assertTrue(WeatherDataId.valueOf("WD-A").compareTo(WeatherDataId.valueOf("WD-B")) < 0);
    }

    // ---- Protected ORM constructors (via subclass/reflection) ----

    @Test
    void ensureAircraftIdProtectedConstructorExercised() {
        final AircraftId id = new AircraftId() { };
        assertNotNull(id);
    }

    @Test
    void ensureAircraftModelIdProtectedConstructorExercised() {
        final AircraftModelId id = new AircraftModelId() { };
        assertNotNull(id);
    }

    @Test
    void ensureAirControlAreaIdProtectedConstructorExercised() {
        final AirControlAreaId id = new AirControlAreaId() { };
        assertNotNull(id);
    }

    @Test
    void ensureAirportIdProtectedConstructorExercised() {
        final AirportId id = new AirportId() { };
        assertNotNull(id);
    }

    @Test
    void ensureAirTransportCompanyIdProtectedConstructorExercised() {
        final AirTransportCompanyId id = new AirTransportCompanyId() { };
        assertNotNull(id);
    }

    @Test
    void ensureEngineModelIdProtectedConstructorExercised() {
        final EngineModelId id = new EngineModelId() { };
        assertNotNull(id);
    }

    @Test
    void ensureFlightIdProtectedConstructorExercised() {
        final FlightId id = new FlightId() { };
        assertNotNull(id);
    }

    @Test
    void ensureFlightPlanIdProtectedConstructorExercised() {
        final FlightPlanId id = new FlightPlanId() { };
        assertNotNull(id);
    }

    @Test
    void ensureFlightRouteIdProtectedConstructorExercised() {
        final FlightRouteId id = new FlightRouteId() { };
        assertNotNull(id);
    }

    @Test
    void ensureNodeIdProtectedConstructorExercised() {
        final NodeId id = new NodeId() { };
        assertNotNull(id);
    }

    @Test
    void ensurePilotIdProtectedConstructorExercised() {
        final PilotId id = new PilotId() { };
        assertNotNull(id);
    }

    @Test
    void ensureSimulationIdProtectedConstructorExercised() {
        final SimulationId id = new SimulationId() { };
        assertNotNull(id);
    }

    @Test
    void ensureSimulationReportIdProtectedConstructorExercised() {
        final SimulationReportId id = new SimulationReportId() { };
        assertNotNull(id);
    }

    @Test
    void ensureUserIdProtectedConstructorExercised() {
        final UserId id = new UserId() { };
        assertNotNull(id);
    }

    @Test
    void ensureWeatherDataIdProtectedConstructorExercised() {
        final WeatherDataId id = new WeatherDataId() { };
        assertNotNull(id);
    }

    @Test
    void ensureCustomerCollaboratorIdProtectedConstructorExercised() {
        final CustomerCollaboratorId id = new CustomerCollaboratorId() { };
        assertNotNull(id);
    }
}
