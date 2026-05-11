package customercollaborator.application;

import auth.domain.AISafeRoles;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;

public class ListCustomerCollaboratorsController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final CustomerCollaboratorRepository collaboratorRepository;

    public ListCustomerCollaboratorsController(final CustomerCollaboratorRepository collaboratorRepository) {
        this.collaboratorRepository = collaboratorRepository;
    }

    public Iterable<CustomerCollaborator> listActiveByAirTransportCompany(final String companyId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return collaboratorRepository.findActiveByAirTransportCompanyId(AirTransportCompanyId.valueOf(companyId));
    }

    public Iterable<CustomerCollaborator> listActiveByAirControlArea(final String areaId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return collaboratorRepository.findActiveByAirControlAreaId(AirControlAreaId.valueOf(areaId));
    }
}