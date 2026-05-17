package customercollaborator.application;

import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.CustomerCollaboratorId;
import shared.domain.UserId;
import user.domain.Role;
import user.domain.User;
import user.repository.UserRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisableCustomerCollaboratorControllerTest {

    private static final String OPERATOR_EMAIL = "dis.operator@test.com";
    private static final String PILOT_EMAIL = "dis.pilot@test.com";
    private static final String PASSWORD = "Password123";
    private static final String COLLAB_EMAIL = "collab.disable@test.com";
    private static final AirTransportCompanyId COMPANY_ID = AirTransportCompanyId.valueOf("dis-company-1");

    private final StubCollaboratorRepository collabRepo = new StubCollaboratorRepository();
    private final StubUserRepository userRepo = new StubUserRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (Exception ignored) { }
        safeRegister(OPERATOR_EMAIL, PASSWORD, AISafeRoles.BACKOFFICE_OPERATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
        safeRegister(COLLAB_EMAIL, PASSWORD, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
    }

    @BeforeEach
    void resetState() {
        collabRepo.clear();
        userRepo.clear();
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    // --- Success ---

    @Test
    void ensureActiveAirTransportCollaboratorIsDisabled() {
        final UserId userId = UserId.valueOf(COLLAB_EMAIL);
        final CustomerCollaborator collab = new CustomerCollaborator(
                userId, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY_ID, null, "Manager");
        final User user = new User(COLLAB_EMAIL, "Collab User", "910000000", null, null,
                "ACTIVE", Set.of(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR), userId);
        collabRepo.add(collab);
        userRepo.add(user);

        new DisableCustomerCollaboratorController(collabRepo, userRepo).disable(COLLAB_EMAIL);

        assertFalse(collab.isActive());
        assertFalse(user.isActive());
    }

    @Test
    void ensureActiveAirControlAreaCollaboratorIsDisabled() {
        final String areaEmail = "area.collab@test.com";
        safeRegister(areaEmail, PASSWORD, AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        final UserId userId = UserId.valueOf(areaEmail);
        final CustomerCollaborator collab = new CustomerCollaborator(
                userId, CustomerType.AIR_CONTROL_AREA, null,
                AirControlAreaId.valueOf("dis-area-1"), "Supervisor");
        final User user = new User(areaEmail, "Area Collab", "920000000", null, null,
                "ACTIVE", Set.of(Role.FLIGHT_CONTROL_OPERATOR), userId);
        collabRepo.add(collab);
        userRepo.add(user);

        new DisableCustomerCollaboratorController(collabRepo, userRepo).disable(areaEmail);

        assertFalse(collab.isActive());
        assertFalse(user.isActive());
    }

    // --- Not found ---

    @Test
    void ensureDisablingNonExistentCollaboratorIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new DisableCustomerCollaboratorController(collabRepo, userRepo)
                        .disable("notfound@test.com"));
    }

    @Test
    void ensureDisablingCollaboratorWithNoUserIsRejected() {
        collabRepo.add(new CustomerCollaborator(
                UserId.valueOf("orphan@test.com"),
                CustomerType.AIR_TRANSPORT_COMPANY, COMPANY_ID, null, "Manager"));

        assertThrows(IllegalArgumentException.class, () ->
                new DisableCustomerCollaboratorController(collabRepo, userRepo)
                        .disable("orphan@test.com"));
    }

    // --- Already disabled ---

    @Test
    void ensureDisablingAlreadyDisabledCollaboratorIsRejected() {
        final String email = "already.dis@test.com";
        safeRegister(email, PASSWORD, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final UserId userId = UserId.valueOf(email);
        final CustomerCollaborator collab = new CustomerCollaborator(
                userId, CustomerType.AIR_TRANSPORT_COMPANY, COMPANY_ID, null, "Manager");
        collab.disable();
        final User user = new User(email, "Already Disabled", "900000000", null, null,
                "DISABLED", Set.of(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR), userId);
        collabRepo.add(collab);
        userRepo.add(user);

        assertThrows(IllegalStateException.class, () ->
                new DisableCustomerCollaboratorController(collabRepo, userRepo).disable(email));
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotDisableCollaborator() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class, () ->
                new DisableCustomerCollaboratorController(collabRepo, userRepo).disable(COLLAB_EMAIL));
    }

    // --- Helpers ---

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (Exception ignored) { }
    }

    static class StubUserRepository
            extends InMemoryDomainRepository<User, UserId>
            implements UserRepository {

        void clear() { data().clear(); }

        void add(final User u) { save(u); }

        @Override
        public Optional<User> findByEmail(final String email) {
            return matchOne(u -> u.email().equals(email));
        }
    }

    static class StubCollaboratorRepository
            extends InMemoryDomainRepository<CustomerCollaborator, CustomerCollaboratorId>
            implements CustomerCollaboratorRepository {

        void clear() { data().clear(); }

        void add(final CustomerCollaborator c) { save(c); }

        @Override
        public Optional<CustomerCollaborator> findByUserId(final UserId id) {
            return matchOne(c -> c.userId().equals(id));
        }

        @Override
        public Iterable<CustomerCollaborator> findActiveByAirTransportCompanyId(final AirTransportCompanyId id) {
            final List<CustomerCollaborator> result = new ArrayList<>();
            match(c -> c.isActive() && id.equals(c.airTransportCompanyId())).forEach(result::add);
            return result;
        }

        @Override
        public Iterable<CustomerCollaborator> findActiveByAirControlAreaId(final AirControlAreaId id) {
            final List<CustomerCollaborator> result = new ArrayList<>();
            match(c -> c.isActive() && id.equals(c.airControlAreaId())).forEach(result::add);
            return result;
        }
    }
}
