package flightPlan.domain;

import org.junit.jupiter.api.Test;
import shared.domain.AirportId;
import shared.domain.FlightPlanId;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanTest {

    private static final FlightPlanId PLAN_ID = FlightPlanId.newId();

    private FlightLeg sampleLeg() {
        return new FlightLeg(1,
                Instant.parse("2027-05-05T08:00:00Z"),
                Instant.parse("2027-05-05T09:30:00Z"),
                4200.0,
                AirportId.valueOf("apt-OPO"),
                AirportId.valueOf("apt-MAD"));
    }

    private FlightPlan validPlan() {
        return new FlightPlan(PLAN_ID, "TP1234", FlightType.REGULAR,
                "A320", "pilot@test.com",
                "AA123", "OPO", "MAD",
                List.of(sampleLeg()));
    }

    @Test
    void ensureFlightPlanIsCreatedAsDraft() {
        final FlightPlan plan = validPlan();
        assertAll(
                () -> assertEquals(PLAN_ID, plan.identity()),
                () -> assertEquals("TP1234", plan.flightDesignator()),
                () -> assertEquals(FlightType.REGULAR, plan.type()),
                () -> assertEquals("A320", plan.aircraftId()),
                () -> assertEquals("pilot@test.com", plan.pilotRef()),
                () -> assertEquals("AA123", plan.routeReference()),
                () -> assertEquals("OPO", plan.routeOrigin()),
                () -> assertEquals("MAD", plan.routeDestination()),
                () -> assertEquals(1, plan.legs().size()),
                () -> assertEquals(Instant.parse("2027-05-05T08:00:00Z"), plan.departureTime()),
                () -> assertEquals(FlightPlanStatus.DRAFT, plan.status()));
    }

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightPlan(null, "TP1234",
                FlightType.REGULAR, "A320", "pilot@test.com", "AA123", "OPO", "MAD", List.of(sampleLeg())));
    }

    @Test
    void ensureEmptyLegsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightPlan(PLAN_ID, "TP1234",
                FlightType.REGULAR, "A320", "pilot@test.com", "AA123", "OPO", "MAD", List.of()));
    }

    @Test
    void ensureFlightPlanCanBeMarkedTestedAndVoided() {
        final FlightPlan plan = validPlan();
        plan.markAsTested();
        assertEquals(FlightPlanStatus.TESTED, plan.status());
        plan.voidTest();
        assertEquals(FlightPlanStatus.VOID, plan.status());
    }

    @Test
    void ensureDraftPlanCannotBeVoided() {
        assertThrows(IllegalStateException.class, () -> validPlan().voidTest());
    }

    @Test
    void ensurePlanEqualsItself() {
        final FlightPlan plan = validPlan();
        assertEquals(plan, plan);
        assertTrue(plan.sameAs(plan));
        assertNotEquals(plan, null);
    }
}
