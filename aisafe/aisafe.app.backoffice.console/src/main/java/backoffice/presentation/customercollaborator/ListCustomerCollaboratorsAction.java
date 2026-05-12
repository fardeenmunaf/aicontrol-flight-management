package backoffice.presentation.customercollaborator;

import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.actions.Action;
import infrastructure.persistence.PersistenceContext;
import user.repository.UserRepository;

public class ListCustomerCollaboratorsAction implements Action {
    @Override
    public boolean execute() {
        final CustomerCollaboratorRepository collaboratorRepository = PersistenceContext.repositories().customerCollaborators();
        final AirTransportCompanyRepository airTransportCompanyRepository = PersistenceContext.repositories().airTransportCompanies();
        final AirControlAreaRepository airControlAreaRepository = PersistenceContext.repositories().airControlAreas();
        final UserRepository userRepository = PersistenceContext.repositories().users();

        return new ListCustomerCollaboratorsUI(
                collaboratorRepository,
                airTransportCompanyRepository,
                airControlAreaRepository,
                userRepository
        ).show();
    }
}