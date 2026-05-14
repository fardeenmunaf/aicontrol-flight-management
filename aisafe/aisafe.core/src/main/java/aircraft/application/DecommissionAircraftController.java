package aircraft.application;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.repositories.AircraftRepository;
import flight.repository.FlightRepository;
import flight.domain.FlightStatus;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import auth.domain.AISafeRoles;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import shared.domain.AircraftId;
import shared.domain.UserId;

public class DecommissionAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepository;
    private final CustomerCollaboratorRepository collaboratorRepository;
    private final AirTransportCompanyRepository companyRepository;
    private final FlightRepository flightRepository;

    public DecommissionAircraftController(final AircraftRepository aircraftRepository,
                                          final CustomerCollaboratorRepository collaboratorRepository,
                                          final AirTransportCompanyRepository companyRepository,
                                          final FlightRepository flightRepository) {
        this.aircraftRepository = aircraftRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.companyRepository = companyRepository;
        this.flightRepository = flightRepository;
    }

    /**
     * Decommission an aircraft from the logged-in user's company fleet.
     * 
     * @param aircraftId the ID of the aircraft to decommission
     * @throws IllegalArgumentException if aircraft not found or already decommissioned
     * @throws IllegalStateException if aircraft has pending flights or doesn't belong to user's company
     */
    public void decommissionAircraft(final String aircraftId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final Aircraft aircraft = aircraftRepository.ofIdentity(AircraftId.valueOf(aircraftId))
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found"));

        // Verify aircraft belongs to logged-in user's company
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

        if (!aircraft.airTransportCompany().identity().equals(company.identity())) {
            throw new IllegalStateException("Aircraft does not belong to your company");
        }

        // Check if aircraft is already decommissioned (before checking for pending flights)
        if (aircraft.operationStatus() == AircraftOperationStatus.DECOMISSIONED) {
            throw new IllegalStateException("Aircraft is already decommissioned");
        }

        // Check for pending flights (scheduled or airborne)
        final var scheduled = flightRepository.findByAircraftAndStatus(aircraft.identity(), FlightStatus.SCHEDULED);
        final var airborne = flightRepository.findByAircraftAndStatus(aircraft.identity(), FlightStatus.AIRBORNE);
        if ((scheduled != null && !scheduled.isEmpty()) || (airborne != null && !airborne.isEmpty())) {
            throw new IllegalStateException("Aircraft has pending flights and cannot be decommissioned");
        }

        aircraft.decommission();
        aircraftRepository.save(aircraft);
    }
}
