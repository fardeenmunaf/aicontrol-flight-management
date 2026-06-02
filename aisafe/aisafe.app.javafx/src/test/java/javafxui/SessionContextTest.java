package javafxui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionContextTest {

    @BeforeEach
    void reset() {
        SessionContext.clear();
    }

    @Test
    void initially_not_authenticated() {
        assertFalse(SessionContext.isAuthenticated());
    }

    @Test
    void initially_not_weather_client() {
        assertFalse(SessionContext.isWeatherClient());
    }

    @Test
    void set_authenticated_marks_authenticated() {
        SessionContext.setAuthenticated(true);
        assertTrue(SessionContext.isAuthenticated());
    }

    @Test
    void clear_resets_to_not_authenticated() {
        SessionContext.setAuthenticated(true);
        SessionContext.clear();
        assertFalse(SessionContext.isAuthenticated());
    }

    @Test
    void clear_resets_weather_client() {
        SessionContext.setWeatherClient(true);
        SessionContext.clear();
        assertFalse(SessionContext.isWeatherClient());
    }

    @Test
    void set_authenticated_false_clears_weather_client() {
        SessionContext.setWeatherClient(true);
        SessionContext.setAuthenticated(false);
        assertFalse(SessionContext.isWeatherClient());
    }

    @Test
    void set_weather_client_marks_weather_client() {
        SessionContext.setWeatherClient(true);
        assertTrue(SessionContext.isWeatherClient());
    }

    @Test
    void set_false_is_not_authenticated() {
        SessionContext.setAuthenticated(true);
        SessionContext.setAuthenticated(false);
        assertFalse(SessionContext.isAuthenticated());
    }
}
