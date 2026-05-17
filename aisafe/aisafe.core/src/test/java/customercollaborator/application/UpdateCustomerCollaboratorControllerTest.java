package customercollaborator.application;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
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

import static org.junit.jupiter.api.Assertions.*;

class UpdateCustomerCollaboratorControllerTest {

    private static final String OPERATOR_EMAIL = "upd.operator@test.com";
    private static final String PILOT_EMAIL = "upd.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final AirTransportCompany TEST_COMPANY =
            new AirTransportCompany("Update Airlines", "UA", "UAL");

    private final StubUserRepository userRepo = new StubUserRepository();
    private final StubCollaboratorRepository collabRepo = new StubCollaboratorRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (Exception ignored) { }
        safeRegister(OPERATOR_EMAIL, PASSWORD, AISafeRoles.BACKOFFICE_OPERATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void resetState() {
        userRepo.clear();
        collabRepo.clear();
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private UpdateCustomerCollaboratorController controller() {
        return new UpdateCustomerCollaboratorController(collabRepo, userRepo);
    }

    private User createUser(final String email) {
        final UserId userId = UserId.valueOf(email);
        final User user = new User(email, "Test User", "910000001", null, null, "ACTIVE",
                Set.of(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR), userId);
        userRepo.add(user);
        return user;
    }

    private CustomerCollaborator createCollaborator(final String email) {
        final UserId userId = UserId.valueOf(email);
        final CustomerCollaborator collab = new CustomerCollaborator(
                userId, CustomerType.AIR_TRANSPORT_COMPANY, TEST_COMPANY.identity(), null, "Manager");
        collabRepo.save(collab);
        return collab;
    }

    // --- Success: update email ---

    @Test
    void ensureEmailCanBeUpdated() {
        final String email = "upd.collab1@test.com";
        createUser(email);
        createCollaborator(email);

        final User result = controller().updateCollaboratorContactInfo(email, "new.email@test.com", null);
        assertNotNull(result);
        assertEquals("new.email@test.com", result.email());
    }

    // --- Success: update phone ---

    @Test
    void ensurePhoneCanBeUpdated() {
        final String email = "upd.collab2@test.com";
        createUser(email);
        createCollaborator(email);

        final User result = controller().updateCollaboratorContactInfo(email, null, "+351912345678");
        assertNotNull(result);
        assertEquals("+351912345678", result.phoneNumber());
    }

    // --- Success: both null/blank (no update) ---

    @Test
    void ensureNoChangeWhenBothFieldsAreNull() {
        final String email = "upd.collab3@test.com";
        createUser(email);
        createCollaborator(email);

        final User result = controller().updateCollaboratorContactInfo(email, null, null);
        assertNotNull(result);
        assertEquals(email, result.email());
    }

    // --- Error: collaborator not found ---

    @Test
    void ensureNonExistentCollaboratorIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().updateCollaboratorContactInfo("nonexistent@test.com", "new@test.com", null));
    }

    // --- Error: collaborator is disabled ---

    @Test
    void ensureDisabledCollaboratorIsRejected() {
        final String email = "upd.collab4@test.com";
        createUser(email);
        final CustomerCollaborator collab = createCollaborator(email);
        collab.disable();
        collabRepo.save(collab);

        assertThrows(IllegalArgumentException.class, () ->
                controller().updateCollaboratorContactInfo(email, "new@test.com", null));
    }

    // --- Error: user not found (collaborator exists but user not in userRepo) ---

    @Test
    void ensureUserNotFoundIsRejected() {
        final String email = "upd.collab6@test.com";
        // Create collaborator but NOT the user in userRepo
        createCollaborator(email);
        // Do NOT create user in userRepo

        assertThrows(IllegalArgumentException.class, () ->
                controller().updateCollaboratorContactInfo(email, "new@test.com", null));
    }

    // --- Success: update both email and phone ---

    @Test
    void ensureBothEmailAndPhoneCanBeUpdated() {
        final String email = "upd.collab7@test.com";
        createUser(email);
        createCollaborator(email);

        final User result = controller().updateCollaboratorContactInfo(email, "updated@test.com", "+351912000000");
        assertNotNull(result);
        assertEquals("updated@test.com", result.email());
        assertEquals("+351912000000", result.phoneNumber());
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotUpdateCollaborator() {
        final String email = "upd.collab5@test.com";
        createUser(email);
        createCollaborator(email);

        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class, () ->
                controller().updateCollaboratorContactInfo(email, "new@test.com", null));
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

        void add(final User user) { save(user); }

        @Override
        public Optional<User> findByEmail(final String email) {
            return matchOne(u -> u.email().equals(email));
        }

        @Override
        public Iterable<User> findAll() {
            return new ArrayList<>(data().values());
        }
    }

    static class StubCollaboratorRepository
            extends InMemoryDomainRepository<CustomerCollaborator, CustomerCollaboratorId>
            implements CustomerCollaboratorRepository {

        void clear() { data().clear(); }

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
            return List.of();
        }
    }
}
