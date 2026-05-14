package aircraftmodel.application;

import aircraft.repositories.AircraftRepository;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.repositories.AircraftModelRepository;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import enginemodel.domain.EngineModel;
import enginemodel.repositories.EngineModelRepository;
import shared.domain.EngineModelId;

import java.util.List;

public class RemoveEngineModelFromAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepository;
    private final EngineModelRepository engineModelRepository;
    private final AircraftRepository aircraftRepository;

    public RemoveEngineModelFromAircraftController(
            final AircraftModelRepository aircraftModelRepository,
            final EngineModelRepository engineModelRepository,
            final AircraftRepository aircraftRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
        this.engineModelRepository = engineModelRepository;
        this.aircraftRepository = aircraftRepository;
    }

    public List<AircraftModel> listAircraftModelsWithMultipleEngines() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        return aircraftModelRepository.findAllAircraftModels().stream()
                .filter(m -> m.supportedEngineModels().size() > 1)
                .toList();
    }

    public List<EngineModel> listRemovableEngineModels(final AircraftModel aircraftModel) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return List.copyOf(aircraftModel.supportedEngineModels());
    }

    public AircraftModel removeEngineModel(
            final AircraftModel aircraftModel,
            final EngineModelId engineModelId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        if (aircraftRepository.existsByAircraftModel(aircraftModel.identity())) {
            throw new IllegalStateException(
                    "Cannot remove engine model: there are aircraft using this aircraft model");
        }

        final EngineModel engineModel = engineModelRepository.ofIdentity(engineModelId)
                .orElseThrow(() -> new IllegalArgumentException("Engine model not found"));

        aircraftModel.removeEngineModel(engineModel);

        return aircraftModelRepository.save(aircraftModel);
    }
}