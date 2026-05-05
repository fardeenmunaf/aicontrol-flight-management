package enginemodel.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import auth.domain.AISafeRoles;
import enginemodel.domain.EngineModel;
import enginemodel.repositories.EngineModelRepository;

public class ListEngineModelsController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final EngineModelRepository engineModelRepository;

    public ListEngineModelsController(final EngineModelRepository engineModelRepository) {
        this.engineModelRepository = engineModelRepository;
    }

    public Iterable<EngineModel> listEngineModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return engineModelRepository.findAll();
    }
}
