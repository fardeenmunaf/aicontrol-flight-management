package customercollaborator.domain;

import org.junit.jupiter.api.Test;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import static org.junit.jupiter.api.Assertions.*;

class CustomerCollaboratorTest {

    private static final UserId USER = UserId.valueOf("user@test.com");
    private static final AirTransportCompanyId COMPANY = AirTransportCompanyId.valueOf("company-1");
    private static final AirControlAreaId AREA = AirControlAreaId.valueOf("area-1");

    // --- Construction ---

    @Test
    void ensureCollaboratorForAirTransportCompanyIsCreatedActive() {
        final CustomerCollaborator c = new CustomerCollaborator(
                USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager");
        assertTrue(c.isActive());
        assertEquals(USER, c.userId());
        assertEquals(CustomerType.AIR_TRANSPORT_COMPANY, c.customerType());
        assertEquals(COMPANY, c.airTransportCompanyId());
        assertNull(c.airControlAreaId());
        assertEquals("Manager", c.position());
        assertNotNull(c.identity());
    }

    @Test
    void ensureCollaboratorForAirControlAreaIsCreatedActive() {
        final CustomerCollaborator c = new CustomerCollaborator(
                USER, CustomerType.AIR_CONTROL_AREA, null, AREA, "Supervisor");
        assertTrue(c.isActive());
        assertEquals(CustomerType.AIR_CONTROL_AREA, c.customerType());
        assertEquals(AREA, c.airControlAreaId());
        assertNull(c.airTransportCompanyId());
        assertEquals("Supervisor", c.position());
    }

    @Test
    void ensurePositionIsTrimmedOnCreation() {
        final CustomerCollaborator c = new CustomerCollaborator(
                USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "  Manager  ");
        assertEquals("Manager", c.position());
    }

    // --- Null / blank validation ---

    @Test
    void ensureNullUserIdIsRejected() {
        assertThrows(Exception.class, () ->
                new CustomerCollaborator(null, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager"));
    }

    @Test
    void ensureNullCustomerTypeIsRejected() {
        assertThrows(Exception.class, () ->
                new CustomerCollaborator(USER, null, COMPANY, null, "Manager"));
    }

    @Test
    void ensureNullPositionIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerCollaborator(USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, null));
    }

    @Test
    void ensureBlankPositionIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerCollaborator(USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "   "));
    }

    // --- Invariant validation ---

    @Test
    void ensureAirTransportCompanyWithNullCompanyIdIsRejected() {
        assertThrows(Exception.class, () ->
                new CustomerCollaborator(USER, CustomerType.AIR_TRANSPORT_COMPANY, null, null, "Manager"));
    }

    @Test
    void ensureAirTransportCompanyWithAreaIdSetIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerCollaborator(USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, AREA, "Manager"));
    }

    @Test
    void ensureAirControlAreaWithNullAreaIdIsRejected() {
        assertThrows(Exception.class, () ->
                new CustomerCollaborator(USER, CustomerType.AIR_CONTROL_AREA, null, null, "Supervisor"));
    }

    @Test
    void ensureAirControlAreaWithCompanyIdSetIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerCollaborator(USER, CustomerType.AIR_CONTROL_AREA, COMPANY, AREA, "Supervisor"));
    }

    // --- Disable ---

    @Test
    void ensureActiveCollaboratorCanBeDisabled() {
        final CustomerCollaborator c = new CustomerCollaborator(
                USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager");
        assertTrue(c.isActive());
        c.disable();
        assertFalse(c.isActive());
    }

    @Test
    void ensureDisablingAlreadyDisabledCollaboratorIsRejected() {
        final CustomerCollaborator c = new CustomerCollaborator(
                USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager");
        c.disable();
        assertThrows(IllegalStateException.class, c::disable);
    }

    // --- Equality / identity ---

    @Test
    void ensureCollaboratorIsEqualToItself() {
        final CustomerCollaborator c = new CustomerCollaborator(
                USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager");
        assertEquals(c, c);
        assertEquals(c.hashCode(), c.hashCode());
    }

    @Test
    void ensureCollaboratorIsNotEqualToNull() {
        final CustomerCollaborator c = new CustomerCollaborator(
                USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager");
        assertNotEquals(c, null);
    }

    @Test
    void ensureTwoDifferentCollaboratorsAreNotEqual() {
        final CustomerCollaborator c1 = new CustomerCollaborator(
                USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager");
        final CustomerCollaborator c2 = new CustomerCollaborator(
                UserId.valueOf("other@test.com"), CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager");
        assertNotEquals(c1, c2);
    }

    @Test
    void ensureCollaboratorSameAsItself() {
        final CustomerCollaborator c = new CustomerCollaborator(
                USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager");
        assertTrue(c.sameAs(c));
    }

    @Test
    void ensureCollaboratorSameAsNullIsFalse() {
        final CustomerCollaborator c = new CustomerCollaborator(
                USER, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY, null, "Manager");
        assertFalse(c.sameAs(null));
    }
}
