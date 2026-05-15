package flightRoute.domain;

import org.junit.jupiter.api.Test;
import shared.domain.FlightRouteId;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FlightRouteTest {

    private static final FlightRouteId ROUTE_ID = FlightRouteId.newId();
    private static final RouteDesignation DESIGNATION = new RouteDesignation("TP123");
    private static final LocalDate ACTIVE_FROM = LocalDate.of(2026, 1, 1);

    private FlightRoute validRoute() {
        return new FlightRoute(ROUTE_ID, DESIGNATION, ACTIVE_FROM);
    }

    // --- Construction: happy path ---

    @Test
    void ensureFlightRouteIsCreatedWithExpectedState() {
        final FlightRoute route = validRoute();
        assertAll(
                () -> assertEquals(ROUTE_ID, route.identity()),
                () -> assertEquals(DESIGNATION, route.designation()),
                () -> assertEquals(ACTIVE_FROM, route.activeFrom()),
                () -> assertNull(route.activeUntil()),
                () -> assertTrue(route.isActiveOn(ACTIVE_FROM))
        );
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(Exception.class, () -> new FlightRoute(null, DESIGNATION, ACTIVE_FROM));
    }

    @Test
    void ensureNullDesignationIsRejected() {
        assertThrows(Exception.class, () -> new FlightRoute(ROUTE_ID, null, ACTIVE_FROM));
    }

    @Test
    void ensureNullActivFromIsRejected() {
        assertThrows(Exception.class, () -> new FlightRoute(ROUTE_ID, DESIGNATION, null));
    }

    // --- isActiveOn ---

    @Test
    void ensureRouteIsActiveOnActiveFromDate() {
        final FlightRoute route = validRoute();
        assertTrue(route.isActiveOn(ACTIVE_FROM));
    }

    @Test
    void ensureRouteIsNotActiveBeforeActiveFromDate() {
        final FlightRoute route = validRoute();
        assertFalse(route.isActiveOn(ACTIVE_FROM.minusDays(1)));
    }

    @Test
    void ensureRouteIsActiveAfterActiveFromDate() {
        final FlightRoute route = validRoute();
        assertTrue(route.isActiveOn(ACTIVE_FROM.plusDays(100)));
    }

    // --- deactivateFrom ---

    @Test
    void ensureRouteCanBeDeactivated() {
        final FlightRoute route = validRoute();
        final LocalDate deactivationDate = ACTIVE_FROM.plusMonths(6);
        route.deactivateFrom(deactivationDate);
        assertEquals(deactivationDate, route.activeUntil());
        assertFalse(route.isActiveOn(deactivationDate.plusDays(1)));
    }

    @Test
    void ensureRouteIsActiveOnDeactivationDate() {
        final FlightRoute route = validRoute();
        final LocalDate deactivationDate = ACTIVE_FROM.plusMonths(6);
        route.deactivateFrom(deactivationDate);
        assertTrue(route.isActiveOn(deactivationDate));
    }

    @Test
    void ensureDeactivationBeforeActiveFromIsRejected() {
        final FlightRoute route = validRoute();
        assertThrows(Exception.class, () -> route.deactivateFrom(ACTIVE_FROM.minusDays(1)));
    }

    @Test
    void ensureNullDeactivationDateIsRejected() {
        final FlightRoute route = validRoute();
        assertThrows(Exception.class, () -> route.deactivateFrom(null));
    }

    @Test
    void ensureAlreadyDeactivatedRouteCannotBeDeactivatedEarlier() {
        final FlightRoute route = validRoute();
        final LocalDate firstDeact = ACTIVE_FROM.plusMonths(6);
        route.deactivateFrom(firstDeact);
        assertThrows(IllegalStateException.class,
                () -> route.deactivateFrom(ACTIVE_FROM.plusMonths(3)));
    }

    // --- equals / hashCode ---

    @Test
    void ensureRouteIsEqualToItself() {
        final FlightRoute route = validRoute();
        assertEquals(route, route);
        assertEquals(route.hashCode(), route.hashCode());
    }

    @Test
    void ensureRouteIsNotEqualToNull() {
        assertNotEquals(validRoute(), null);
    }

    @Test
    void ensureTwoDifferentRoutesAreNotEqual() {
        final FlightRoute r1 = validRoute();
        final FlightRoute r2 = new FlightRoute(FlightRouteId.newId(), new RouteDesignation("BA99"), ACTIVE_FROM);
        assertNotEquals(r1, r2);
    }

    // --- toString ---

    @Test
    void ensureToStringContainsDesignation() {
        final FlightRoute route = validRoute();
        assertTrue(route.toString().contains("TP123"));
    }

    @Test
    void ensureFlightRouteSameAsItself() {
        final FlightRoute route = validRoute();
        assertTrue(route.sameAs(route));
    }

    @Test
    void ensureFlightRouteSameAsNullIsFalse() {
        final FlightRoute route = validRoute();
        assertFalse(route.sameAs(null));
    }

    @Test
    void ensureIsActiveReturnsTrueWhenActiveNow() {
        // Create a route that started in the past (still active)
        final FlightRoute route = new FlightRoute(ROUTE_ID, DESIGNATION, LocalDate.of(2020, 1, 1));
        assertTrue(route.isActive());
    }

    @Test
    void ensureIsActiveReturnsFalseWhenDeactivated() {
        // Create a route that started and ended in the past
        final FlightRoute route = new FlightRoute(ROUTE_ID, DESIGNATION, LocalDate.of(2020, 1, 1));
        route.deactivateFrom(LocalDate.of(2021, 1, 1));
        assertFalse(route.isActive());
    }
}
