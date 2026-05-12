package backoffice.presentation.customercollaborator;

import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.actions.Action;
import infrastructure.persistence.PersistenceContext;

public class DisableCustomerCollaboratorAction implements Action {
    @Override
    public boolean execute() {
        final CustomerCollaboratorRepository collaboratorRepository = PersistenceContext.repositories().customerCollaborators();
        final AirTransportCompanyRepository airTransportCompanyRepository = PersistenceContext.repositories().airTransportCompanies();
        final AirControlAreaRepository airControlAreaRepository = PersistenceContext.repositories().airControlAreas();

        return new DisableCustomerCollaboratorUI(
                collaboratorRepository,
                airTransportCompanyRepository,
                airControlAreaRepository
        ).show();
    }
}