package customercollaborator.application;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import customercollaborator.domain.CustomerCollaborator;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterCustomerCollaboratorControllerTest {

    private static final String OPERATOR_EMAIL = "reg.operator@test.com";
    private static final String PILOT_EMAIL = "reg.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final AirTransportCompany TEST_COMPANY =
            new AirTransportCompany("Test Airlines", "TA", "TAL");
    private static final AirControlArea TEST_AREA =
            new AirControlArea("PT-TEST", 100.0, "Test Boundaries");

    private final StubUserRepository userRepo = new StubUserRepository();
    private final StubCollaboratorRepository collabRepo = new StubCollaboratorRepository();
    private final StubAirTransportCompanyRepository companyRepo = new StubAirTransportCompanyRepository();
    private final StubAirControlAreaRepository areaRepo = new StubAirControlAreaRepository();

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

    private RegisterCustomerCollaboratorController newController() {
        return new RegisterCustomerCollaboratorController(
                userRepo, collabRepo, companyRepo, areaRepo);
    }

    // --- Success cases ---

    @Test
    void ensureCollaboratorForAirTransportCompanyIsRegistered() {
        final CustomerCollaborator result = newController().registerForAirTransportCompany(
                TEST_COMPANY.identity().toString(),
                "new1@company.com", PASSWORD, "John Doe", "910000001", "Manager");
        assertNotNull(result);
        assertTrue(result.isActive());
    }

    @Test
    void ensureCollaboratorForAirControlAreaIsRegistered() {
        final CustomerCollaborator result = newController().registerForAirControlArea(
                TEST_AREA.identity().toString(),
                "new2@area.com", PASSWORD, "Jane Doe", "920000001", "Supervisor");
        assertNotNull(result);
        assertTrue(result.isActive());
    }

    @Test
    void ensureSingleWordNameIsHandled() {
        final CustomerCollaborator result = newController().registerForAirTransportCompany(
                TEST_COMPANY.identity().toString(),
                "singlename@company.com", PASSWORD, "John", "930000001", "Pilot");
        assertNotNull(result);
    }

    // --- Duplicate email ---

    @Test
    void ensureDuplicateEmailIsRejected() {
        userRepo.preLoad("existing@test.com");
        assertThrows(IllegalArgumentException.class, () ->
                newController().registerForAirTransportCompany(
                        TEST_COMPANY.identity().toString(),
                        "existing@test.com", PASSWORD, "John Doe", "910000001", "Manager"));
    }

    // --- Non-existent customer ---

    @Test
    void ensureNonExistentCompanyIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                newController().registerForAirTransportCompany(
                        "non-existent-id", "new3@test.com", PASSWORD, "John Doe", "910000001", "Manager"));
    }

    @Test
    void ensureNonExistentAreaIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                newController().registerForAirControlArea(
                        "non-existent-id", "new4@test.com", PASSWORD, "Jane Doe", "920000001", "Supervisor"));
    }

    // --- Field validation ---

    @Test
    void ensureBlankEmailIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                newController().registerForAirTransportCompany(
                        TEST_COMPANY.identity().toString(), "", PASSWORD, "John Doe", "910000001", "Manager"));
    }

    @Test
    void ensureBlankPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                newController().registerForAirTransportCompany(
                        TEST_COMPANY.identity().toString(), "a@b.com", "", "John Doe", "910000001", "Manager"));
    }

    @Test
    void ensureBlankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                newController().registerForAirTransportCompany(
                        TEST_COMPANY.identity().toString(), "a@b.com", PASSWORD, "", "910000001", "Manager"));
    }

    @Test
    void ensureBlankPhoneIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                newController().registerForAirTransportCompany(
                        TEST_COMPANY.identity().toString(), "a@b.com", PASSWORD, "John Doe", "", "Manager"));
    }

    @Test
    void ensureBlankPositionIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                newController().registerForAirTransportCompany(
                        TEST_COMPANY.identity().toString(), "a@b.com", PASSWORD, "John Doe", "910000001", ""));
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotRegisterCollaborator() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class, () ->
                newController().registerForAirTransportCompany(
                        TEST_COMPANY.identity().toString(),
                        "unauth@test.com", PASSWORD, "John Doe", "910000001", "Manager"));
    }

    // --- Stubs ---

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

        void preLoad(final String email) {
            final UserId id = UserId.valueOf(email);
            save(new User(email, "Existing", "000000000", null, null, "ACTIVE",
                    Set.of(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR), id));
        }

        @Override
        public Optional<User> findByEmail(final String email) {
            return matchOne(u -> u.email().equals(email));
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
            final List<CustomerCollaborator> result = new ArrayList<>();
            match(c -> c.isActive() && id.equals(c.airControlAreaId())).forEach(result::add);
            return result;
        }
    }

    static class StubAirTransportCompanyRepository
            extends InMemoryDomainRepository<AirTransportCompany, AirTransportCompanyId>
            implements AirTransportCompanyRepository {

        StubAirTransportCompanyRepository() { save(TEST_COMPANY); }

        @Override public List<AirTransportCompany> findByName(final String n) { return List.of(); }
        @Override public List<AirTransportCompany> findByIataCode(final String c) { return List.of(); }
        @Override public List<AirTransportCompany> findByIcaoCode(final String c) { return List.of(); }
    }

    static class StubAirControlAreaRepository
            extends InMemoryDomainRepository<AirControlArea, AirControlAreaId>
            implements AirControlAreaRepository {

        StubAirControlAreaRepository() { save(TEST_AREA); }

        @Override public List<AirControlArea> findByAreaCode(final String c) { return List.of(); }
        @Override public List<AirControlArea> findByMinimumRequiredArrivalFuelGreaterThanEqual(final double v) { return List.of(); }
        @Override public List<AirControlArea> findByGeographicBoundaries(final String g) { return List.of(); }
    }
}
