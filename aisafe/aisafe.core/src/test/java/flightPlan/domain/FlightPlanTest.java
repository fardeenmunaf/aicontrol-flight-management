package flightPlan.domain;

import org.junit.jupiter.api.Test;
import shared.domain.FlightPlanId;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanTest {

    private static final FlightPlanId PLAN_ID = FlightPlanId.newId();
    private static final String VALID_JSON = "{\"aircraftId\":\"A320\",\"routeReference\":\"TP123\"}";
    private static final Instant DEP = Instant.parse("2027-06-01T08:00:00Z");

    private FlightPlan validPlan() {
        return new FlightPlan(PLAN_ID, VALID_JSON, "A320", "pilot@test.com", "TP123", DEP);
    }

    // --- Construction: happy path ---

    @Test
    void ensureFlightPlanIsCreatedAsDraft() {
        final FlightPlan plan = validPlan();
        assertAll(
                () -> assertEquals(PLAN_ID, plan.identity()),
                () -> assertEquals(VALID_JSON, plan.flightPlanJson()),
                () -> assertEquals("A320", plan.aircraftId()),
                () -> assertEquals("pilot@test.com", plan.pilotRef()),
                () -> assertEquals("TP123", plan.routeReference()),
                () -> assertEquals(DEP, plan.departureTime()),
                () -> assertEquals(FlightPlanStatus.DRAFT, plan.status())
        );
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(null, VALID_JSON, "A320", "pilot@test.com", "TP123", DEP));
    }

    @Test
    void ensureNullJsonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(PLAN_ID, null, "A320", "pilot@test.com", "TP123", DEP));
    }

    @Test
    void ensureEmptyJsonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(PLAN_ID, "", "A320", "pilot@test.com", "TP123", DEP));
    }

    // --- markAsTested ---

    @Test
    void ensureFlightPlanCanBeMarkedAsTested() {
        final FlightPlan plan = validPlan();
        plan.markAsTested();
        assertEquals(FlightPlanStatus.TESTED, plan.status());
    }

    // --- voidTest ---

    @Test
    void ensureTestedFlightPlanCanBeVoided() {
        final FlightPlan plan = validPlan();
        plan.markAsTested();
        plan.voidTest();
        assertEquals(FlightPlanStatus.VOID, plan.status());
    }

    @Test
    void ensureDraftFlightPlanCannotBeVoided() {
        final FlightPlan plan = validPlan();
        assertThrows(IllegalStateException.class, plan::voidTest);
    }

    // --- equals / hashCode ---

    @Test
    void ensurePlanIsEqualToItself() {
        final FlightPlan plan = validPlan();
        assertEquals(plan, plan);
        assertEquals(plan.hashCode(), plan.hashCode());
    }

    @Test
    void ensurePlanIsNotEqualToNull() {
        assertNotEquals(validPlan(), null);
    }

    @Test
    void ensureFlightPlanSameAsItself() {
        final FlightPlan plan = validPlan();
        assertTrue(plan.sameAs(plan));
    }

    @Test
    void ensureFlightPlanSameAsNullIsFalse() {
        final FlightPlan plan = validPlan();
        assertFalse(plan.sameAs(null));
    }
}
