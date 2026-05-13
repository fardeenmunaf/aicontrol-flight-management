package user.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginControllerTest extends UserApplicationTestSupport {

    @BeforeAll
    static void init() {
        setupAuth();
    }

    @Test
    void ensureLoginControllerAuthenticatesAndLogsOut() {
        final LoginController controller = new LoginController();

        assertTrue(controller.authenticate(LOGIN_EMAIL, LOGIN_PASSWORD));
        assertFalse(controller.authenticate(LOGIN_EMAIL, "wrong-password"));
        assertDoesNotThrow(controller::logout);
    }
}


