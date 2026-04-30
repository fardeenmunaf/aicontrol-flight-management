package backoffice;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthenticatedException;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;

import java.util.Set;

public final class BackofficeApp {

    private static final String ADMIN_EMAIL = "admin@aisafe.com";
    private static final String ADMIN_PASSWORD = "Admin1234!";
    private static final String PILOT_EMAIL = "pilot@aisafe.com";
    private static final String PILOT_PASSWORD = "Pilot1234!";

    private BackofficeApp() {
        // utility
    }

    public static void main(final String[] args) {
        AuthzRegistry.configure(
                new InMemoryUserRepository(),
                new AISafePasswordPolicy(),
                new PlainTextEncoder()
        );

        seedUsers();
        runUs030Checks();
    }

    private static void seedUsers() {
        registerUser(ADMIN_EMAIL, ADMIN_PASSWORD, "System", "Admin", AISafeRoles.ADMIN);
        registerUser(PILOT_EMAIL, PILOT_PASSWORD, "Default", "Pilot", AISafeRoles.PILOT);
        registerUser("fco@aisafe.com", "Fco1234!", "Default", "FCO", AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        registerUser("weather@aisafe.com", "Weather1234!", "Weather", "Person", AISafeRoles.WEATHER_PERSON);
        registerUser("bo@aisafe.com", "Operator1234!", "Backoffice", "Operator", AISafeRoles.BACKOFFICE_OPERATOR);
    }

    private static void registerUser(final String email, final String password,
                                     final String firstName, final String lastName,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        AuthzRegistry.userService().registerNewUser(
                email,
                password,
                firstName,
                lastName,
                email,
                Set.of(role)
        );
    }

    private static void runUs030Checks() {
        final AuthorizationService authz = AuthzRegistry.authorizationService();

        logResult("AC01 unauthenticated access blocked", expectUnauthenticated(authz));

        final boolean adminLogin = AuthzRegistry.authenticationService()
                .authenticate(ADMIN_EMAIL, ADMIN_PASSWORD)
                .isPresent();
        logResult("AC02 login with email/password", adminLogin);
        logResult("AC04 admin can access admin action", expectAuthorized(authz, AISafeRoles.ADMIN));

        authz.clearSession();
        logResult("AC07 logout clears session", !authz.hasSession());

        final boolean pilotLogin = AuthzRegistry.authenticationService()
                .authenticate(PILOT_EMAIL, PILOT_PASSWORD)
                .isPresent();
        logResult("Pilot login works", pilotLogin);
        logResult("AC04 pilot blocked from admin action", expectUnauthorized(authz, AISafeRoles.ADMIN));

        AuthzRegistry.userService()
                .userOfIdentity(Username.valueOf(PILOT_EMAIL))
                .ifPresent(AuthzRegistry.userService()::deactivateUser);
        authz.clearSession();

        final boolean disabledLogin = AuthzRegistry.authenticationService()
                .authenticate(PILOT_EMAIL, PILOT_PASSWORD)
                .isPresent();
        logResult("AC06 disabled user cannot authenticate", !disabledLogin);
    }

    private static boolean expectUnauthenticated(final AuthorizationService authz) {
        try {
            authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN);
            return false;
        } catch (final UnauthenticatedException ex) {
            return true;
        }
    }

    private static boolean expectUnauthorized(final AuthorizationService authz,
                                              final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            authz.ensureAuthenticatedUserHasAnyOf(role);
            return false;
        } catch (final UnauthorizedException ex) {
            return true;
        }
    }

    private static boolean expectAuthorized(final AuthorizationService authz,
                                            final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            authz.ensureAuthenticatedUserHasAnyOf(role);
            return true;
        } catch (final RuntimeException ex) {
            return false;
        }
    }

    private static void logResult(final String label, final boolean ok) {
        System.out.println("[US030] " + label + ": " + (ok ? "OK" : "FAIL"));
    }
}

