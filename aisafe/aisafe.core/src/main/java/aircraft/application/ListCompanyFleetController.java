package aircraft.application;

import aircraft.dto.AircraftDTO;
import aircraft.repositories.AircraftRepository;
import auth.domain.AISafeRoles;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Controller for listing an air transport company's aircraft fleet.
 * Only authenticated ATCC can list their own company's fleet.
 *
 * @author LAPR4 Team
 */
public class ListCompanyFleetController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepository;
    private final CustomerCollaboratorRepository collaboratorRepository;

    public ListCompanyFleetController(
            final AircraftRepository aircraftRepository,
            final CustomerCollaboratorRepository collaboratorRepository) {
        this.aircraftRepository = aircraftRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    /**
     * Lists all aircraft in the authenticated ATCC's company fleet.
     * Includes both active and decommissioned aircraft.
     *
     * @return list of aircraft DTOs for the company
     * @throws eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException
     *         if user is not authenticated or not an ATCC
     * @throws IllegalStateException if ATCC is not associated with a company
     */
    public List<AircraftDTO> listCompanyFleet() {
        // Ensure user is authenticated and has ATCC role
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        // Get the authenticated user
        final SystemUser authenticatedUser = authz.session()
                .orElseThrow(IllegalStateException::new)
                .authenticatedUser();

        // Find the ATCC's company association
        final var collaborator = collaboratorRepository
                .findByUserId(UserId.valueOf(authenticatedUser.identity().toString()))
                .orElseThrow(() ->
                    new IllegalStateException("ATCC is not associated with a company"));

        final AirTransportCompanyId companyId = collaborator.airTransportCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("ATCC is not associated with an air transport company");
        }

        // Retrieve all aircraft for the company
        return StreamSupport.stream(
                aircraftRepository.findByAirTransportCompanyId(companyId).spliterator(), false)
                .map(this::toDTO)
                .toList();
    }

    /**
     * Lists all aircraft in a specific company's fleet (if authorized).
     * Only for BACKOFFICE operators (not available for regular ATCC).
     *
     * @param companyId the company ID
     * @return list of aircraft DTOs for the company
     * @throws eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException
     *         if user is not authorized to list fleets of other companies
     */
    public List<AircraftDTO> listCompanyFleetAsBackoffice(final String companyId) {
        // This method is for administrative use
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final AirTransportCompanyId id = AirTransportCompanyId.valueOf(companyId);
        return StreamSupport.stream(
                aircraftRepository.findByAirTransportCompanyId(id).spliterator(), false)
                .map(this::toDTO)
                .toList();
    }

    /**
     * Converts an Aircraft domain object to a DTO.
     *
     * @param aircraft the aircraft to convert
     * @return the aircraft DTO
     */
    private AircraftDTO toDTO(final aircraft.domain.Aircraft aircraft) {
        final String cabinConfigStr = String.format(
                "Economy: %d, Business: %d, First: %d",
                aircraft.cabinConfiguration().economyClassSeats(),
                aircraft.cabinConfiguration().businessClassSeats(),
                aircraft.cabinConfiguration().firstClassSeats()
        );

        return new AircraftDTO(
                aircraft.registrationNumber(),
                aircraft.aircraftModel().modelName(),
                aircraft.operationStatus().toString(),
                cabinConfigStr,
                aircraft.registrationCountry()
        );
    }
}

