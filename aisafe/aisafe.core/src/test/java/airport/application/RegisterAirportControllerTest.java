package airport.application;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airport.domain.Airport;
import airport.repositories.AirportRepository;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AirControlAreaId;
import shared.domain.AirportId;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RegisterAirportControllerTest {

    private static final String OPERATOR_EMAIL = "airport.operator@test.com";
    private static final String PILOT_EMAIL = "airport.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final AirControlArea TEST_AREA =
            new AirControlArea("PT-NORTH", 100.0, "North Portugal");

    private final StubAirportRepository airportRepo = new StubAirportRepository();
    private final StubAirControlAreaRepository areaRepo = new StubAirControlAreaRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (Exception ignored) { }
        safeRegister(OPERATOR_EMAIL, PASSWORD, AISafeRoles.BACKOFFICE_OPERATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void resetState() {
        airportRepo.clear();
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private RegisterAirportController ctrl() {
        return new RegisterAirportController(airportRepo, areaRepo);
    }

    // --- Success ---

    @Test
    void ensureAirportWithValidDataIsRegistered() {
        final Airport result = ctrl().registerAirport(
                "Porto Airport", "Porto", "Portugal",
                "OPO", "LPPR", 69.0, 41.24, -8.67,
                TEST_AREA.identity().toString());
        assertNotNull(result);
        assertEquals("OPO", result.iataCode());
        assertEquals("LPPR", result.icaoCode());
    }

    @Test
    void ensureCodesAreNormalizedOnRegister() {
        final Airport result = ctrl().registerAirport(
                "Lisbon Airport", "Lisbon", "Portugal",
                "lis", "lppt", 114.0, 38.77, -9.13,
                TEST_AREA.identity().toString());
        assertEquals("LIS", result.iataCode());
        assertEquals("LPPT", result.icaoCode());
    }

    // --- Duplicate codes ---

    @Test
    void ensureDuplicateIataCodeIsRejected() {
        ctrl().registerAirport("Porto Airport", "Porto", "Portugal",
                "OPO", "LPPR", 69.0, 41.24, -8.67, TEST_AREA.identity().toString());
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Other Airport", "City", "Country",
                        "OPO", "ABCD", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureDuplicateIcaoCodeIsRejected() {
        ctrl().registerAirport("Porto Airport", "Porto", "Portugal",
                "OPO", "LPPR", 69.0, 41.24, -8.67, TEST_AREA.identity().toString());
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Other Airport", "City", "Country",
                        "LIS", "LPPR", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    // --- Non-existent area ---

    @Test
    void ensureNonExistentAirControlAreaIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test Airport", "City", "Country",
                        "XYZ", "XYZW", 0.0, 0.0, 0.0, "NON-EXISTENT-AREA"));
    }

    // --- Invalid IATA/ICAO format ---

    @Test
    void ensureIataWithWrongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "AB", "LPPR", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureIcaoWithWrongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "LPP", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureIataWithDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "A1C", "LPPR", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    // --- Invalid coordinates ---

    @Test
    void ensureLatitudeAboveRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "ABCD", 0.0, 91.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureLongitudeAboveRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "ABCD", 0.0, 0.0, 181.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureLatitudeBelowRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "ABCD", 0.0, -91.0, 0.0, TEST_AREA.identity().toString()));
    }

    // --- Blank / null fields ---

    @Test
    void ensureBlankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("", "City", "Country",
                        "ABC", "ABCD", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureBlankTownIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "", "Country",
                        "ABC", "ABCD", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureBlankCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "",
                        "ABC", "ABCD", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureBlankIataIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "", "ABCD", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureBlankIcaoIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureNullElevationIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "ABCD", null, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureNullLatitudeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "ABCD", 0.0, null, 0.0, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureNullLongitudeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "ABCD", 0.0, 0.0, null, TEST_AREA.identity().toString()));
    }

    @Test
    void ensureBlankAreaIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "ABCD", 0.0, 0.0, 0.0, ""));
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotRegisterAirport() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class, () ->
                ctrl().registerAirport("Test", "City", "Country",
                        "ABC", "ABCD", 0.0, 0.0, 0.0, TEST_AREA.identity().toString()));
    }

    // --- Helpers ---

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (Exception ignored) { }
    }

    static class StubAirportRepository
            extends InMemoryDomainRepository<Airport, AirportId>
            implements AirportRepository {

        void clear() { data().clear(); }

        @Override
        public Optional<Airport> findByIataCode(final String iataCode) {
            return matchOne(a -> a.iataCode().equalsIgnoreCase(iataCode));
        }

        @Override
        public Optional<Airport> findByIcaoCode(final String icaoCode) {
            return matchOne(a -> a.icaoCode().equalsIgnoreCase(icaoCode));
        }
    }

    static class StubAirControlAreaRepository
            extends InMemoryDomainRepository<AirControlArea, AirControlAreaId>
            implements AirControlAreaRepository {

        StubAirControlAreaRepository() { save(TEST_AREA); }

        @Override public List<AirControlArea> findByAreaCode(final String c) { return List.of(); }
        @Override public List<AirControlArea> findByMinimumRequiredArrivalFuelGreaterThanEqual(final double v) { return List.of(); }
        @Override public List<AirControlArea> findByGeographicBoundaries(final String g) { return List.of(); }
    }
}
