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

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListCustomerCollaboratorsControllerTest {

    private static final String OPERATOR_EMAIL = "list.operator@test.com";
    private static final String PILOT_EMAIL = "list.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final AirTransportCompanyId COMPANY_ID = AirTransportCompanyId.valueOf("list-company-1");
    private static final AirControlAreaId AREA_ID = AirControlAreaId.valueOf("list-area-1");

    private final StubCollaboratorRepository repo = new StubCollaboratorRepository();

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
        repo.clear();
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    // --- List by Air Transport Company ---

    @Test
    void ensureActiveCollaboratorsForCompanyAreListed() {
        repo.add(activeCompanyCollaborator("collab1@co.com"));
        repo.add(activeCompanyCollaborator("collab2@co.com"));

        final List<CustomerCollaborator> result = toList(
                new ListCustomerCollaboratorsController(repo)
                        .listActiveByAirTransportCompany(COMPANY_ID.toString()));

        assertEquals(2, result.size());
    }

    @Test
    void ensureDisabledCompanyCollaboratorsAreNotListed() {
        final CustomerCollaborator active = activeCompanyCollaborator("active@co.com");
        final CustomerCollaborator disabled = activeCompanyCollaborator("disabled@co.com");
        disabled.disable();
        repo.add(active);
        repo.add(disabled);

        final List<CustomerCollaborator> result = toList(
                new ListCustomerCollaboratorsController(repo)
                        .listActiveByAirTransportCompany(COMPANY_ID.toString()));

        assertEquals(1, result.size());
        assertEquals("active@co.com", result.get(0).userId().toString());
    }

    @Test
    void ensureEmptyListIsReturnedWhenCompanyHasNoCollaborators() {
        final List<CustomerCollaborator> result = toList(
                new ListCustomerCollaboratorsController(repo)
                        .listActiveByAirTransportCompany(COMPANY_ID.toString()));

        assertTrue(result.isEmpty());
    }

    // --- List by Air Control Area ---

    @Test
    void ensureActiveCollaboratorsForAreaAreListed() {
        repo.add(activeAreaCollaborator("fco1@area.com"));

        final List<CustomerCollaborator> result = toList(
                new ListCustomerCollaboratorsController(repo)
                        .listActiveByAirControlArea(AREA_ID.toString()));

        assertEquals(1, result.size());
    }

    @Test
    void ensureDisabledAreaCollaboratorsAreNotListed() {
        final CustomerCollaborator disabled = activeAreaCollaborator("disabled.area@test.com");
        disabled.disable();
        repo.add(disabled);

        final List<CustomerCollaborator> result = toList(
                new ListCustomerCollaboratorsController(repo)
                        .listActiveByAirControlArea(AREA_ID.toString()));

        assertTrue(result.isEmpty());
    }

    @Test
    void ensureEmptyListIsReturnedWhenAreaHasNoCollaborators() {
        final List<CustomerCollaborator> result = toList(
                new ListCustomerCollaboratorsController(repo)
                        .listActiveByAirControlArea(AREA_ID.toString()));

        assertTrue(result.isEmpty());
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotListCompanyCollaborators() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class, () ->
                new ListCustomerCollaboratorsController(repo)
                        .listActiveByAirTransportCompany(COMPANY_ID.toString()));
    }

    @Test
    void ensureUnauthorizedUserCannotListAreaCollaborators() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class, () ->
                new ListCustomerCollaboratorsController(repo)
                        .listActiveByAirControlArea(AREA_ID.toString()));
    }

    // --- Helpers ---

    private CustomerCollaborator activeCompanyCollaborator(final String email) {
        return new CustomerCollaborator(UserId.valueOf(email),
                CustomerType.AIR_TRANSPORT_COMPANY, COMPANY_ID, null, "Manager");
    }

    private CustomerCollaborator activeAreaCollaborator(final String email) {
        return new CustomerCollaborator(UserId.valueOf(email),
                CustomerType.AIR_CONTROL_AREA, null, AREA_ID, "Supervisor");
    }

    private static <T> List<T> toList(final Iterable<T> iter) {
        final List<T> list = new ArrayList<>();
        for (final T t : iter) list.add(t);
        return list;
    }

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (Exception ignored) { }
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
