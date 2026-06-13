package pilot.domain;

import org.junit.jupiter.api.Test;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import static org.junit.jupiter.api.Assertions.*;

class PilotCompanyAssignmentTest {

    private static final UserId PILOT_ID = UserId.valueOf("pilot@test.com");
    private static final AirTransportCompanyId COMPANY_ID = AirTransportCompanyId.valueOf("company-001");

    private PilotCompanyAssignment valid() {
        return new PilotCompanyAssignment(PILOT_ID, COMPANY_ID);
    }

    // --- US075: Construction happy path ---

    @Test
    void ensureAssignmentIsCreatedWithExpectedState() {
        final PilotCompanyAssignment a = valid();
        assertAll(
                () -> assertEquals(PILOT_ID, a.pilotUserId()),
                () -> assertEquals(COMPANY_ID, a.companyId()),
                () -> assertEquals(PILOT_ID, a.identity())
        );
    }

    // --- US075: Null validation (AC04 — assignment must be valid to persist) ---

    @Test
    void ensureNullPilotUserIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PilotCompanyAssignment(null, COMPANY_ID));
    }

    @Test
    void ensureNullCompanyIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PilotCompanyAssignment(PILOT_ID, null));
    }

    // --- US076: identity used to scope roster queries ---

    @Test
    void ensureIdentityMatchesPilotUserId() {
        final PilotCompanyAssignment a = valid();
        assertEquals(a.pilotUserId(), a.identity());
    }

    // --- AC05: Duplicate assignments prevented by aggregate identity ---

    @Test
    void ensureTwoAssignmentsWithSamePilotAreEqual() {
        final PilotCompanyAssignment a1 = new PilotCompanyAssignment(PILOT_ID, COMPANY_ID);
        final PilotCompanyAssignment a2 = new PilotCompanyAssignment(PILOT_ID, AirTransportCompanyId.valueOf("other-co"));
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void ensureAssignmentsWithDifferentPilotsAreNotEqual() {
        final PilotCompanyAssignment a1 = new PilotCompanyAssignment(PILOT_ID, COMPANY_ID);
        final PilotCompanyAssignment a2 = new PilotCompanyAssignment(
                UserId.valueOf("other@test.com"), COMPANY_ID);
        assertNotEquals(a1, a2);
    }

    @Test
    void ensureAssignmentIsEqualToItself() {
        final PilotCompanyAssignment a = valid();
        assertEquals(a, a);
    }

    @Test
    void ensureAssignmentIsNotEqualToNull() {
        assertNotEquals(valid(), null);
    }

    @Test
    void ensureSameAsItself() {
        final PilotCompanyAssignment a = valid();
        assertTrue(a.sameAs(a));
    }

    @Test
    void ensureSameAsNullIsFalse() {
        assertFalse(valid().sameAs(null));
    }

    // --- ORM no-arg constructor ---

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        final PilotCompanyAssignment a = new PilotCompanyAssignment() {};
        assertNotNull(a);
    }
}
