package airtransportcompany.application;

import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AirTransportCompanyId;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ListAirTransportCompaniesControllerTest {

    private static final String OPERATOR_EMAIL = "list.company.operator@test.com";
    private static final String PILOT_EMAIL = "list.company.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private final StubAirTransportCompanyRepository repo = new StubAirTransportCompanyRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(
                    new InMemoryUserRepository(),
                    new AISafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (final Exception ignored) {}
        safeRegister(OPERATOR_EMAIL, PASSWORD, AISafeRoles.BACKOFFICE_OPERATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void authenticate() {
        repo.clear();
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private ListAirTransportCompaniesController controller() {
        return new ListAirTransportCompaniesController(repo);
    }

    @Test
    void ensureEmptyRepositoryReturnsEmptyList() {
        final List<AirTransportCompany> result = controller().allCompanies();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureAllCompaniesAreReturned() {
        repo.save(new AirTransportCompany("Company A", "CA", "CAL"));
        repo.save(new AirTransportCompany("Company B", "CB", "CBL"));

        final List<AirTransportCompany> result = controller().allCompanies();
        assertEquals(2, result.size());
    }

    @Test
    void ensureSingleCompanyIsReturned() {
        repo.save(new AirTransportCompany("Single Co", "SC", "SCO"));
        final List<AirTransportCompany> result = controller().allCompanies();
        assertEquals(1, result.size());
        assertEquals("Single Co", result.get(0).name());
    }

    @Test
    void ensureUnauthorizedUserCannotListCompanies() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class, () -> controller().allCompanies());
    }

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (final Exception ignored) {}
    }

    static class StubAirTransportCompanyRepository
            extends InMemoryDomainRepository<AirTransportCompany, AirTransportCompanyId>
            implements AirTransportCompanyRepository {

        void clear() { data().clear(); }

        @Override public List<AirTransportCompany> findByName(final String n) { return List.of(); }
        @Override public List<AirTransportCompany> findByIataCode(final String c) { return List.of(); }
        @Override public List<AirTransportCompany> findByIcaoCode(final String c) { return List.of(); }
    }
}
