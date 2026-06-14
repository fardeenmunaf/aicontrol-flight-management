package pilot.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PilotMessageCodeTest {

    @Test
    void ensureCommtestCodeIsZero() {
        assertEquals((byte) 0, PilotMessageCode.COMMTEST.code());
    }

    @Test
    void ensureDisconnCodeIsOne() {
        assertEquals((byte) 1, PilotMessageCode.DISCONN.code());
    }

    @Test
    void ensureAckCodeIsTwo() {
        assertEquals((byte) 2, PilotMessageCode.ACK.code());
    }

    @Test
    void ensureErrCodeIsThree() {
        assertEquals((byte) 3, PilotMessageCode.ERR.code());
    }

    @Test
    void ensureAuthCodeIsFour() {
        assertEquals((byte) 4, PilotMessageCode.AUTH.code());
    }

    @Test
    void ensureListMyPlansCodeIsTwenty() {
        assertEquals((byte) 20, PilotMessageCode.LIST_MY_PLANS.code());
    }

    @Test
    void ensureCreatePlanCodeIsTwentyOne() {
        assertEquals((byte) 21, PilotMessageCode.CREATE_PLAN.code());
    }

    @Test
    void ensureValidatePlanCodeIsTwentyTwo() {
        assertEquals((byte) 22, PilotMessageCode.VALIDATE_PLAN.code());
    }

    @Test
    void ensureAddWeatherCodeIsTwentyThree() {
        assertEquals((byte) 23, PilotMessageCode.ADD_WEATHER.code());
    }

    @Test
    void ensureListFleetCodeIsTwentyFour() {
        assertEquals((byte) 24, PilotMessageCode.LIST_FLEET.code());
    }

    @Test
    void ensureListRoutesCodeIsTwentyFive() {
        assertEquals((byte) 25, PilotMessageCode.LIST_ROUTES.code());
    }

    @Test
    void ensureFromResolvesAllValidCodes() {
        for (final PilotMessageCode code : PilotMessageCode.values()) {
            assertEquals(code, PilotMessageCode.from(code.code()));
        }
    }

    @Test
    void ensureFromUnknownCodeThrows() {
        assertThrows(IllegalArgumentException.class, () -> PilotMessageCode.from((byte) 99));
    }

    @Test
    void ensureFromNegativeCodeThrows() {
        assertThrows(IllegalArgumentException.class, () -> PilotMessageCode.from((byte) -1));
    }
}
