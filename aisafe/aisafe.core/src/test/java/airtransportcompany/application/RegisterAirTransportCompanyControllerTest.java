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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterAirTransportCompanyControllerTest {

    private static final String OPERATOR_EMAIL = "reg.company.operator@test.com";
    private static final String PILOT_EMAIL = "reg.company.pilot@test.com";
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

    private RegisterAirTransportCompanyController controller() {
        return new RegisterAirTransportCompanyController(repo);
    }

    // --- Happy path ---

    @Test
    void ensureCompanyIsRegistered() {
        final AirTransportCompany company = controller().registerCompany("Test Airlines", "TA", "TAL");
        assertNotNull(company);
        assertEquals("Test Airlines", company.name());
        assertEquals("TA", company.iataCode());
        assertEquals("TAL", company.icaoCode());
    }

    @Test
    void ensureCompanyWithTwoLetterIcaoIsRegistered() {
        final AirTransportCompany company = controller().registerCompany("Short Airlines", "SA", "SA");
        assertNotNull(company);
        assertEquals("SA", company.icaoCode());
    }

    // --- Duplicate code rejection ---

    @Test
    void ensureDuplicateIataCodeIsRejected() {
        controller().registerCompany("First Airlines", "FA", "FAL");
        assertThrows(IllegalArgumentException.class,
                () -> controller().registerCompany("Second Airlines", "FA", "SAL"));
    }

    @Test
    void ensureDuplicateIcaoCodeIsRejected() {
        controller().registerCompany("First Airlines", "FA", "FAL");
        assertThrows(IllegalArgumentException.class,
                () -> controller().registerCompany("Second Airlines", "SE", "FAL"));
    }

    // --- Invalid arguments ---

    @Test
    void ensureBlankNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().registerCompany("", "BA", "BAL"));
    }

    @Test
    void ensureInvalidIataCodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().registerCompany("Invalid Airlines", "ABC", "IAL"));
    }

    @Test
    void ensureNumericIataCodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().registerCompany("Invalid Airlines", "12", "IAL"));
    }

    @Test
    void ensureInvalidIcaoCodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().registerCompany("Invalid Airlines", "IA", "IAAL"));
    }

    @Test
    void ensureBlankIataCodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().registerCompany("Test Airlines", "", "TAL"));
    }

    @Test
    void ensureBlankIcaoCodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().registerCompany("Test Airlines", "TA", ""));
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotRegisterCompany() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class,
                () -> controller().registerCompany("Unauth Airlines", "UA", "UAL"));
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

        @Override
        public List<AirTransportCompany> findByName(final String name) {
            final List<AirTransportCompany> result = new ArrayList<>();
            match(c -> c.name().equals(name)).forEach(result::add);
            return result;
        }

        @Override
        public List<AirTransportCompany> findByIataCode(final String iataCode) {
            final List<AirTransportCompany> result = new ArrayList<>();
            match(c -> c.iataCode().equals(iataCode)).forEach(result::add);
            return result;
        }

        @Override
        public List<AirTransportCompany> findByIcaoCode(final String icaoCode) {
            final List<AirTransportCompany> result = new ArrayList<>();
            match(c -> c.icaoCode().equals(icaoCode)).forEach(result::add);
            return result;
        }
    }
}
