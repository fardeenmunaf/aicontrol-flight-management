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
    void set_false_is_not_authenticated() {
        SessionContext.setAuthenticated(true);
        SessionContext.setAuthenticated(false);
        assertFalse(SessionContext.isAuthenticated());
    }
}
