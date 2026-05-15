package aircraft.application;

import aircraft.domain.Aircraft;
import aircraft.domain.CabinConfiguration;
import aircraft.domain.AircraftOperationStatus;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.repositories.AircraftModelRepository;
import aircraft.repositories.AircraftRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import auth.domain.AISafeRoles;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import shared.domain.AircraftModelId;
import shared.domain.EngineModelId;
import shared.domain.UserId;
import enginemodel.domain.EngineModel;
import enginemodel.repositories.EngineModelRepository;

public class RegisterAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepository;
    private final AircraftModelRepository aircraftModelRepository;
    private final CustomerCollaboratorRepository collaboratorRepository;
    private final AirTransportCompanyRepository companyRepository;
    private final EngineModelRepository engineModelRepository;

    public RegisterAircraftController(final AircraftRepository aircraftRepository,
                                      final AircraftModelRepository aircraftModelRepository,
                                      final CustomerCollaboratorRepository collaboratorRepository,
                                      final AirTransportCompanyRepository companyRepository,
                                      final EngineModelRepository engineModelRepository) {
        this.aircraftRepository = aircraftRepository;
        this.aircraftModelRepository = aircraftModelRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.companyRepository = companyRepository;
        this.engineModelRepository = engineModelRepository;
    }

    /**
     * Register a new aircraft instance in the system
     */
    public Aircraft registerAircraft(final String registrationNumber,
                                     final String registrationCountry,
                                     final AircraftOperationStatus operationStatus,
                                     final int flightCrewCount,
                                     final String aircraftModelId,
                                     final String engineModelId,
                                     final int economySeats,
                                     final int businessSeats,
                                     final int firstSeats) {

        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration number must be provided");
        }
        if (registrationCountry == null || registrationCountry.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration country must be provided");
        }
        if (operationStatus == null) {
            throw new IllegalArgumentException("Operation status must be provided");
        }

        if (aircraftRepository.findByRegistrationNumber(registrationNumber).isPresent()) {
            throw new IllegalArgumentException("Aircraft with registration '" + registrationNumber + "' already exists");
        }

        final AircraftModel model = aircraftModelRepository
                .ofIdentity(AircraftModelId.valueOf(aircraftModelId))
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model does not exist"));

        final EngineModel engine = engineModelRepository
                .ofIdentity(EngineModelId.valueOf(engineModelId))
                .orElseThrow(() -> new IllegalArgumentException("Engine model does not exist"));

        final int totalSeats = economySeats + businessSeats + firstSeats;
        final int totalPeople = totalSeats + flightCrewCount;

        // Weight-based estimate: payload budget from MZFW after empty weight and safety margin.
        final double payloadBudgetKg = model.maximumZeroFuelWeight() - model.emptyWeight() - 500.0;
        final int estimatedMaxPeople = (int) Math.floor(Math.max(0.0, payloadBudgetKg) / 100.0);

        if (totalPeople > estimatedMaxPeople) {
            throw new IllegalArgumentException(
                    "Total people (crew + passengers = " + totalPeople + ") exceed estimated limit ("
                            + estimatedMaxPeople + ") for this model"
            );
        }

        final var session = AuthzRegistry.authorizationService().session().orElseThrow(IllegalStateException::new);
        final var userId = UserId.valueOf(session.authenticatedUser().identity().toString());
        final CustomerCollaborator collaborator = collaboratorRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Logged in user is not a customer collaborator"));

        if (collaborator.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
            throw new IllegalStateException("Logged in user is not assigned to an air transport company");
        }

        final AirTransportCompany company = companyRepository
            .ofIdentity(collaborator.airTransportCompanyId())
            .orElseThrow(() -> new IllegalStateException("Air transport company not found"));

        final CabinConfiguration cabin = new CabinConfiguration(economySeats, firstSeats, businessSeats);

        final Aircraft aircraft = new Aircraft(
            registrationNumber.trim(),
            registrationCountry.trim(),
            operationStatus,
            flightCrewCount,
            model,
            engine,
            cabin,
            company
        );

        return aircraftRepository.save(aircraft);
    }
}
