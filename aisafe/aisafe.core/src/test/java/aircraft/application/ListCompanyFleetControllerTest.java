package aircraft.application;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.domain.CabinConfiguration;
import aircraft.repositories.AircraftRepository;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import enginemodel.domain.*;
import enginemodel.domain.EngineModelMaker;
import airtransportcompany.domain.AirTransportCompany;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ListCompanyFleetController.
 * Tests the functionality of listing an air transport company's aircraft fleet
 * with proper authorization and access control.
 */
class ListCompanyFleetControllerTest {

    private static final String ATCC_EMAIL = "atcc@company.com";
    private static final String ATCC2_EMAIL = "atcc2@company2.com";
    private static final String FCO_EMAIL = "fco@test.com";
    private static final String PASSWORD = "Password123";

    private final StubAircraftRepository aircraftRepo = new StubAircraftRepository();
    private final StubCollaboratorRepository collaboratorRepo = new StubCollaboratorRepository();

    private AircraftModel aircraftModel;
    private CabinConfiguration cabinConfig;
    private AirTransportCompany company;
    private AirTransportCompany company2;
    private EngineModel engineModel;

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (Exception ignored) { }
        safeRegister(ATCC_EMAIL, PASSWORD, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        safeRegister(ATCC2_EMAIL, PASSWORD, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        safeRegister(FCO_EMAIL, PASSWORD, AISafeRoles.FLIGHT_CONTROL_OPERATOR);
    }

    @BeforeEach
    void resetState() {
        aircraftRepo.clear();
        collaboratorRepo.clear();
        AuthzRegistry.authenticationService().authenticate(ATCC_EMAIL, PASSWORD);

        // Setup test data
        aircraftModel = new AircraftModel(
                AircraftType.PASSENGER,
                41413.0, 79015.0, 62730.0, 26020.0, 12500.0, 840.0,
                124.6, 35.8, 0.024, 1.25, "B737-800", 2,
                new AircraftModelMaker("Boeing", "USA")
        );

        engineModel = new EngineModel(
                "CFM56-7B", EngineType.TURBOFAN, 121.0, 0.38, 121000.0, 98000.0, 0.62,
                new EngineModelMaker("CFM", "France"),
                new FuelProperties(FuelType.JET_A, 0.80, 43.0)
        );
        aircraftModel.addSupportedEngineModel(engineModel);

        cabinConfig = new CabinConfiguration(150, 12, 6);

        company = new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
        company2 = new AirTransportCompany("Company 2", "C2", "CMP");

        // Associate ATCC with company — use actual identity so findByAirTransportCompanyId matches
        collaboratorRepo.add(new CustomerCollaborator(
                UserId.valueOf(ATCC_EMAIL),
                CustomerType.AIR_TRANSPORT_COMPANY, company.identity(), null, "Manager"
        ));

        // Associate ATCC2 with company2
        collaboratorRepo.add(new CustomerCollaborator(
                UserId.valueOf(ATCC2_EMAIL),
                CustomerType.AIR_TRANSPORT_COMPANY, company2.identity(), null, "Manager"
        ));
    }

    // --- TC01: Successfully list company fleet ---

    @Test
    void ensureSingleAircraftCanBeListed() {
        final Aircraft aircraft = new Aircraft(
                "CS-TPA", "Portugal", AircraftOperationStatus.IN_SERVICE,
                2, aircraftModel, engineModel, cabinConfig, company
        );
        aircraftRepo.add(aircraft);

        final List<aircraft.dto.AircraftDTO> result =
                new ListCompanyFleetController(aircraftRepo, collaboratorRepo)
                        .listCompanyFleet();

        assertEquals(1, result.size());
        assertNotNull(result.get(0).registrationNumber);
        assertEquals("B737-800", result.get(0).model);
        assertEquals("IN_SERVICE", result.get(0).operationalStatus);
        assertTrue(result.get(0).cabinConfig.contains("150"));
        assertEquals("Portugal", result.get(0).registrationCountry);
    }

    @Test
    void ensureMultipleAircraftCanBeListed() {
        aircraftRepo.add(new Aircraft("CS-TPA", "Portugal", AircraftOperationStatus.IN_SERVICE,
                2, aircraftModel, engineModel, cabinConfig, company));
        aircraftRepo.add(new Aircraft("CS-TPB", "Portugal", AircraftOperationStatus.MAINTENANCE,
                2, aircraftModel, engineModel, cabinConfig, company));
        aircraftRepo.add(new Aircraft("CS-TPC", "Portugal", AircraftOperationStatus.IN_SERVICE,
                2, aircraftModel, engineModel, cabinConfig, company));

        final List<aircraft.dto.AircraftDTO> result =
                new ListCompanyFleetController(aircraftRepo, collaboratorRepo)
                        .listCompanyFleet();

        assertEquals(3, result.size());
    }

    // --- TC02: Empty fleet message ---

    @Test
    void ensureEmptyFleetReturnsEmptyList() {
        final List<aircraft.dto.AircraftDTO> result =
                new ListCompanyFleetController(aircraftRepo, collaboratorRepo)
                        .listCompanyFleet();

        assertTrue(result.isEmpty());
    }

    // --- TC03: Fleet list includes both active and retired aircraft ---

    @Test
    void ensureFleetIncludesRetiredAircraft() {
        aircraftRepo.add(new Aircraft("CS-TPA", "Portugal", AircraftOperationStatus.IN_SERVICE,
                2, aircraftModel, engineModel, cabinConfig, company));
        aircraftRepo.add(new Aircraft("CS-TPB", "Portugal", AircraftOperationStatus.DECOMISSIONED,
                2, aircraftModel, engineModel, cabinConfig, company));

        final List<aircraft.dto.AircraftDTO> result =
                new ListCompanyFleetController(aircraftRepo, collaboratorRepo)
                        .listCompanyFleet();

        assertEquals(2, result.size());

        final var retired = result.stream()
                .filter(a -> "DECOMISSIONED".equals(a.operationalStatus))
                .findFirst();

        assertTrue(retired.isPresent());
    }

    // --- TC04: ATCC cannot view another company's fleet ---

    @Test
    void ensureATCCCannotViewAnotherCompanyFleet() {
        // Add aircraft to company 2
        aircraftRepo.add(new Aircraft("A1", "Country", AircraftOperationStatus.IN_SERVICE,
                2, aircraftModel, engineModel, cabinConfig, company2));

        // ATCC1 is logged in but assigned to company 1
        // They should not see aircraft from company 2
        final List<aircraft.dto.AircraftDTO> result =
                new ListCompanyFleetController(aircraftRepo, collaboratorRepo)
                        .listCompanyFleet();

        // ATCC1 should see an empty list (no aircraft from their company)
        assertTrue(result.isEmpty());

        // Switch to ATCC2 and verify they see aircraft from company 2
        AuthzRegistry.authenticationService().authenticate(ATCC2_EMAIL, PASSWORD);

        final List<aircraft.dto.AircraftDTO> result2 =
                new ListCompanyFleetController(aircraftRepo, collaboratorRepo)
                        .listCompanyFleet();

        assertEquals(1, result2.size());
    }

    // --- TC05: Non-ATCC cannot list fleet ---

    @Test
    void ensureFCOCannotListFleet() {
        aircraftRepo.add(new Aircraft("CS-TPA", "Portugal", AircraftOperationStatus.IN_SERVICE,
                2, aircraftModel, engineModel, cabinConfig, company));

        AuthzRegistry.authenticationService().authenticate(FCO_EMAIL, PASSWORD);

        assertThrows(RuntimeException.class, () ->
                new ListCompanyFleetController(aircraftRepo, collaboratorRepo)
                        .listCompanyFleet());
    }

    @Test
    void ensureUnauthenticatedUserCannotListFleet() {
        AuthzRegistry.authorizationService().clearSession();

        assertThrows(RuntimeException.class, () ->
                new ListCompanyFleetController(aircraftRepo, collaboratorRepo)
                        .listCompanyFleet());
    }

    // --- AC02: Entry shows minimum required information ---

    @Test
    void ensureFleetEntryShowsAllRequiredFields() {
        final Aircraft aircraft = new Aircraft(
                "CS-TPA", "Portugal", AircraftOperationStatus.IN_SERVICE,
                2, aircraftModel, engineModel, cabinConfig, company
        );
        aircraftRepo.add(aircraft);

        final List<aircraft.dto.AircraftDTO> result =
                new ListCompanyFleetController(aircraftRepo, collaboratorRepo)
                        .listCompanyFleet();

        assertEquals(1, result.size());
        final aircraft.dto.AircraftDTO dto = result.get(0);

        // Check all required fields are present
        assertNotNull(dto.registrationNumber);
        assertNotNull(dto.model);
        assertNotNull(dto.operationalStatus);
        assertNotNull(dto.cabinConfig);

        assertEquals("B737-800", dto.model);
        assertEquals("IN_SERVICE", dto.operationalStatus);
        assertTrue(dto.cabinConfig.contains("Economy"));
    }

    // --- Helpers ---

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (Exception ignored) { }
    }

    static class StubAircraftRepository
            extends InMemoryDomainRepository<Aircraft, AircraftId>
            implements AircraftRepository {

        void clear() { data().clear(); }
        void add(final Aircraft a) { save(a); }

        @Override
        public List<Aircraft> findByCabinConfiguration(final CabinConfiguration cabinConfiguration) {
            return List.of();
        }

        @Override
        public boolean existsByAircraftModel(final AircraftModelId aircraftModelId) {
            return false;
        }

        @Override
        public List<Aircraft> findByOperationStatus(final String operationStatus) {
            return List.of();
        }

        @Override
        public List<Aircraft> findByRegistrationCountry(final String registrationCountry) {
            return List.of();
        }

        @Override
        public Optional<Aircraft> findByRegistrationNumber(final String registrationNumber) {
            return Optional.empty();
        }

        @Override
        public List<Aircraft> findByAirTransportCompanyId(final AirTransportCompanyId companyId) {
            final List<Aircraft> result = new ArrayList<>();
            match(a -> a.airTransportCompany() != null && a.airTransportCompany().identity().equals(companyId))
                    .forEach(result::add);
            return result;
        }
    }

    static class StubCollaboratorRepository
            extends InMemoryDomainRepository<CustomerCollaborator, CustomerCollaboratorId>
            implements CustomerCollaboratorRepository {

        void clear() { data().clear(); }
        void add(final CustomerCollaborator c) { save(c); }

        @Override
        public Optional<CustomerCollaborator> findByUserId(final UserId userId) {
            return matchOne(c -> c.userId().equals(userId));
        }

        @Override
        public Iterable<CustomerCollaborator> findActiveByAirTransportCompanyId(final AirTransportCompanyId id) {
            return List.of();
        }

        @Override
        public Iterable<CustomerCollaborator> findActiveByAirControlAreaId(final AirControlAreaId id) {
            return List.of();
        }
    }
}

