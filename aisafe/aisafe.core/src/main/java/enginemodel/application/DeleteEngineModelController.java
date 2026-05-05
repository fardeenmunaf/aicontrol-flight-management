package enginemodel.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import auth.domain.AISafeRoles;
import enginemodel.domain.EngineModel;
import enginemodel.repositories.EngineModelRepository;

public class DeleteEngineModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final EngineModelRepository engineModelRepository;

    public DeleteEngineModelController(final EngineModelRepository engineModelRepository) {
        this.engineModelRepository = engineModelRepository;
    }

    public Iterable<EngineModel> activeEngineModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return engineModelRepository.findAll();
    }

    public void deleteEngineModel(final EngineModel engineModel) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        engineModelRepository.delete(engineModel);
    }
}
