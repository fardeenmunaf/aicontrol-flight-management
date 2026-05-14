package weatherData.application;

import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weatherData.dto.WeatherDataDTO;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterWeatherDataControllerTest {

    private static final String OPERATOR_EMAIL = "weather.operator@test.com";
    private static final String PILOT_EMAIL = "weather.pilot@test.com";
    private static final String PASSWORD = "Password123";

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(
                    new InMemoryUserRepository(),
                    new AISafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (Exception ignored) {
        }

        safeRegister(OPERATOR_EMAIL, PASSWORD, AISafeRoles.BACKOFFICE_OPERATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void authenticate() {
        AuthzRegistry.authenticationService()
                .authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private RegisterWeatherDataController ctrl() {
        return new RegisterWeatherDataController();
    }

    private WeatherDataDTO validDto() {
        return new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.of(2025, 6, 15, 12, 0),
                180.0,
                15.0,
                22.5,
                1013.25
        );
    }

    // --- Success ---

    @Test
    void ensureValidWeatherDataIsRegistered() {
        assertTrue(ctrl().registerWeatherData(validDto()));
    }

    @Test
    void ensureWeatherDataWithBoundaryWindDirectionZeroIsAccepted() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                0.0,
                10.0,
                20.0,
                1010.0
        );

        assertTrue(ctrl().registerWeatherData(dto));
    }

    @Test
    void ensureWeatherDataWithBoundaryWindDirection360IsAccepted() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                360.0,
                10.0,
                20.0,
                1010.0
        );

        assertTrue(ctrl().registerWeatherData(dto));
    }

    @Test
    void ensureWeatherDataWithMinTemperatureIsAccepted() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                90.0,
                5.0,
                -100.0,
                1010.0
        );

        assertTrue(ctrl().registerWeatherData(dto));
    }

    @Test
    void ensureWeatherDataWithMaxTemperatureIsAccepted() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                90.0,
                5.0,
                100.0,
                1010.0
        );

        assertTrue(ctrl().registerWeatherData(dto));
    }

    // --- Invalid air control area ---

    @Test
    void ensureNullAirControlAreaIsRejected() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                null,
                LocalDateTime.now(),
                90.0,
                10.0,
                20.0,
                1010.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ctrl().registerWeatherData(dto)
        );
    }

    @Test
    void ensureBlankAirControlAreaIsRejected() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "",
                LocalDateTime.now(),
                90.0,
                10.0,
                20.0,
                1010.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ctrl().registerWeatherData(dto)
        );
    }

    // --- Invalid wind speed ---

    @Test
    void ensureNegativeWindSpeedIsRejected() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                90.0,
                -1.0,
                20.0,
                1010.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ctrl().registerWeatherData(dto)
        );
    }

    // --- Invalid wind direction ---

    @Test
    void ensureNegativeWindDirectionIsRejected() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                -1.0,
                10.0,
                20.0,
                1010.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ctrl().registerWeatherData(dto)
        );
    }

    @Test
    void ensureWindDirectionAbove360IsRejected() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                361.0,
                10.0,
                20.0,
                1010.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ctrl().registerWeatherData(dto)
        );
    }

    // --- Invalid pressure ---

    @Test
    void ensureZeroPressureIsRejected() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                90.0,
                10.0,
                20.0,
                0.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ctrl().registerWeatherData(dto)
        );
    }

    @Test
    void ensureNegativePressureIsRejected() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                90.0,
                10.0,
                20.0,
                -5.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ctrl().registerWeatherData(dto)
        );
    }

    // --- Invalid temperature ---

    @Test
    void ensureTemperatureBelowMinIsRejected() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                90.0,
                10.0,
                -101.0,
                1010.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ctrl().registerWeatherData(dto)
        );
    }

    @Test
    void ensureTemperatureAboveMaxIsRejected() {

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                90.0,
                10.0,
                101.0,
                1010.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ctrl().registerWeatherData(dto)
        );
    }

    private static void safeRegister(
            final String email,
            final String password,
            final eapli.framework.infrastructure.authz.domain.model.Role role) {

        try {
            AuthzRegistry.userService().registerNewUser(
                    email,
                    password,
                    "Test",
                    "User",
                    email,
                    Set.of(role));
        } catch (Exception ignored) {
        }
    }
}