package flightPlan.domain;

import org.junit.jupiter.api.Test;
import shared.domain.AirportId;
import shared.domain.FlightPlanId;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanTest {

    private static final FlightPlanId PLAN_ID = FlightPlanId.newId();
    private static final Instant DEP_TIME = Instant.parse("2026-06-01T08:00:00Z");
    private static final Instant LEG1_ARR = Instant.parse("2026-06-01T10:00:00Z");
    private static final Instant LEG2_DEP = LEG1_ARR;
    private static final Instant LEG2_ARR = Instant.parse("2026-06-01T12:00:00Z");

    private static final AirportId LIS = AirportId.valueOf("LIS");
    private static final AirportId MAD = AirportId.valueOf("MAD");
    private static final AirportId BCN = AirportId.valueOf("BCN");

    private FlightPlan validPlan() {
        return new FlightPlan(PLAN_ID, DEP_TIME, 5000.0, 150, 1000.0);
    }

    // --- Construction: happy path ---

    @Test
    void ensureFlightPlanIsCreatedAsDraft() {
        final FlightPlan plan = validPlan();
        assertAll(
                () -> assertEquals(PLAN_ID, plan.identity()),
                () -> assertEquals(DEP_TIME, plan.departureDateTime()),
                () -> assertEquals(5000.0, plan.fuelQuantity()),
                () -> assertEquals(150, plan.passengerLoad()),
                () -> assertEquals(1000.0, plan.cargoLoad()),
                () -> assertEquals(FlightPlanStatus.DRAFT, plan.status()),
                () -> assertTrue(plan.legs().isEmpty())
        );
    }

    @Test
    void ensureZeroPassengerLoadIsAccepted() {
        assertDoesNotThrow(() -> new FlightPlan(PLAN_ID, DEP_TIME, 5000.0, 0, 0.0));
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(Exception.class, () -> new FlightPlan(null, DEP_TIME, 5000.0, 150, 1000.0));
    }

    @Test
    void ensureNullDepartureDateTimeIsRejected() {
        assertThrows(Exception.class, () -> new FlightPlan(PLAN_ID, null, 5000.0, 150, 1000.0));
    }

    @Test
    void ensureZeroFuelQuantityIsRejected() {
        assertThrows(Exception.class, () -> new FlightPlan(PLAN_ID, DEP_TIME, 0.0, 150, 1000.0));
    }

    @Test
    void ensureNegativeFuelQuantityIsRejected() {
        assertThrows(Exception.class, () -> new FlightPlan(PLAN_ID, DEP_TIME, -1.0, 150, 1000.0));
    }

    @Test
    void ensureNegativePassengerLoadIsRejected() {
        assertThrows(Exception.class, () -> new FlightPlan(PLAN_ID, DEP_TIME, 5000.0, -1, 1000.0));
    }

    // --- addLeg ---

    @Test
    void ensureFirstLegCanBeAdded() {
        final FlightPlan plan = validPlan();
        final FlightLeg leg = new FlightLeg(1, DEP_TIME, LEG1_ARR, 2500.0, LIS, MAD);
        plan.addLeg(leg);
        assertEquals(1, plan.legs().size());
    }

    @Test
    void ensureConnectedLegsCanBeAdded() {
        final FlightPlan plan = validPlan();
        final FlightLeg leg1 = new FlightLeg(1, DEP_TIME, LEG1_ARR, 2500.0, LIS, MAD);
        final FlightLeg leg2 = new FlightLeg(2, LEG2_DEP, LEG2_ARR, 2500.0, MAD, BCN);
        plan.addLeg(leg1);
        plan.addLeg(leg2);
        assertEquals(2, plan.legs().size());
    }

    @Test
    void ensureDisconnectedLegsAreRejected() {
        final FlightPlan plan = validPlan();
        final FlightLeg leg1 = new FlightLeg(1, DEP_TIME, LEG1_ARR, 2500.0, LIS, MAD);
        final FlightLeg leg2 = new FlightLeg(2, LEG2_DEP, LEG2_ARR, 2500.0, BCN, LIS); // BCN != MAD
        plan.addLeg(leg1);
        assertThrows(IllegalArgumentException.class, () -> plan.addLeg(leg2));
    }

    // --- markAsTested ---

    @Test
    void ensureFlightPlanWithLegCanBeMarkedAsTested() {
        final FlightPlan plan = validPlan();
        plan.addLeg(new FlightLeg(1, DEP_TIME, LEG1_ARR, 2500.0, LIS, MAD));
        plan.markAsTested();
        assertEquals(FlightPlanStatus.TESTED, plan.status());
    }

    @Test
    void ensureEmptyFlightPlanCannotBeMarkedAsTested() {
        final FlightPlan plan = validPlan();
        assertThrows(IllegalStateException.class, plan::markAsTested);
    }

    // --- voidTest ---

    @Test
    void ensureTestedFlightPlanCanBeVoided() {
        final FlightPlan plan = validPlan();
        plan.addLeg(new FlightLeg(1, DEP_TIME, LEG1_ARR, 2500.0, LIS, MAD));
        plan.markAsTested();
        plan.voidTest();
        assertEquals(FlightPlanStatus.VOID, plan.status());
    }

    @Test
    void ensureDraftFlightPlanCannotBeVoided() {
        final FlightPlan plan = validPlan();
        assertThrows(IllegalStateException.class, plan::voidTest);
    }

    @Test
    void ensureAddingLegToTestedPlanResetsToDraft() {
        final FlightPlan plan = validPlan();
        plan.addLeg(new FlightLeg(1, DEP_TIME, LEG1_ARR, 2500.0, LIS, MAD));
        plan.markAsTested();
        assertEquals(FlightPlanStatus.TESTED, plan.status());
        plan.addLeg(new FlightLeg(2, LEG2_DEP, LEG2_ARR, 2500.0, MAD, BCN));
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
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

    @Test
    void ensureNegativeCargoLoadIsRejected() {
        assertThrows(Exception.class, () -> new FlightPlan(PLAN_ID, DEP_TIME, 5000.0, 150, -1.0));
    }
}
