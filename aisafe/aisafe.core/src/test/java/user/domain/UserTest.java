package user.domain;

import org.junit.jupiter.api.Test;
import shared.domain.UserId;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private static final UserId USER_ID = UserId.valueOf("user@aisafe.com");

    @Test
    void ensureUserIsCreatedWithExpectedState() {
        final User user = new User(
                "user@aisafe.com",
                "John Doe",
                "+351912345678",
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 6, 30),
                "ACTIVE",
                EnumSet.of(Role.ADMIN, Role.PILOT),
                USER_ID);

        assertAll(
                () -> assertEquals("user@aisafe.com", user.email()),
                () -> assertEquals("John Doe", user.name()),
                () -> assertEquals("+351912345678", user.phoneNumber()),
                () -> assertEquals(LocalDate.of(2026, 12, 31), user.activeSecurityClearanceExpiry()),
                () -> assertEquals(LocalDate.of(2026, 6, 30), user.skillsAssessmentDueDate()),
                () -> assertEquals("ACTIVE", user.status()),
                () -> assertTrue(user.isActive()),
                () -> assertEquals(USER_ID, user.userId()),
                () -> assertEquals(Set.of(Role.ADMIN, Role.PILOT), user.roles())
        );
    }

    @Test
    void ensureDisableAndEnableToggleStatus() {
        final User user = new User(
                "toggle@aisafe.com",
                "Toggle User",
                "+351900000000",
                null,
                null,
                "ACTIVE",
                Set.of(Role.BACKOFFICE_OPERATOR),
                UserId.valueOf("toggle@aisafe.com"));

        user.disable();
        assertAll(
                () -> assertEquals("DISABLED", user.status()),
                () -> assertFalse(user.isActive())
        );

        user.enable();
        assertAll(
                () -> assertEquals("ACTIVE", user.status()),
                () -> assertTrue(user.isActive())
        );
    }

    @Test
    void ensureEmailCanBeUpdatedWithValidFormat() {
        final User user = baseUser();

        user.updateEmail("updated@aisafe.com");

        assertEquals("updated@aisafe.com", user.email());
    }

    @Test
    void ensureInvalidEmailsAreRejectedOnUpdate() {
        final User user = baseUser();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> user.updateEmail(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> user.updateEmail("   ")),
                () -> assertThrows(IllegalArgumentException.class, () -> user.updateEmail("invalid-email"))
        );
    }

    @Test
    void ensurePhoneNumberCanBeUpdatedWithValidFormat() {
        final User user = baseUser();

        user.updatePhoneNumber("+351 912 345 678");

        assertEquals("+351 912 345 678", user.phoneNumber());
    }

    @Test
    void ensureInvalidPhoneNumbersAreRejectedOnUpdate() {
        final User user = baseUser();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> user.updatePhoneNumber(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> user.updatePhoneNumber("   ")),
                () -> assertThrows(IllegalArgumentException.class, () -> user.updatePhoneNumber("12"))
        );
    }

    @Test
    void ensureRolesAreExposedAsDefensiveCopy() {
        final User user = baseUser();
        final Set<Role> roles = user.roles();

        assertThrows(UnsupportedOperationException.class, () -> roles.add(Role.ADMIN));
        assertEquals(Set.of(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR), user.roles());
    }

    @Test
    void ensureEqualsAndHashCodeUseIdentity() {
        final User user1 = baseUser();
        final User user2 = baseUser();
        final User other = new User(
                "other@aisafe.com",
                "Other User",
                "+351900000001",
                null,
                null,
                "ACTIVE",
                Set.of(Role.PILOT),
                UserId.valueOf("other@aisafe.com"));

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1, other);
        assertNotEquals(null, user1);
    }

    @Test
    void ensureRoleDesignationsAreHumanReadable() {
        assertAll(
                () -> assertEquals("Admin", Role.ADMIN.designation()),
                () -> assertEquals("Backoffice Operator", Role.BACKOFFICE_OPERATOR.designation()),
                () -> assertEquals("ATCC", Role.AIR_TRANSPORT_COMPANY_COLLABORATOR.designation()),
                () -> assertEquals("Pilot", Role.PILOT.designation()),
                () -> assertEquals("FCO", Role.FLIGHT_CONTROL_OPERATOR.designation()),
                () -> assertEquals("Weather Person", Role.WEATHER_PERSON.designation())
        );
    }

    private static User baseUser() {
        return new User(
                "base@aisafe.com",
                "Base User",
                "+351910000000",
                null,
                null,
                "ACTIVE",
                Set.of(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR),
                UserId.valueOf("base@aisafe.com"));
    }
}

