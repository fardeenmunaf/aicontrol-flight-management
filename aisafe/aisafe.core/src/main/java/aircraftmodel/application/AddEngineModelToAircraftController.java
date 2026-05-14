package aircraftmodel.application;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.repositories.AircraftModelRepository;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import enginemodel.domain.EngineModel;
import enginemodel.repositories.EngineModelRepository;

import java.util.ArrayList;
import java.util.List;

public class AddEngineModelToAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepository;
    private final EngineModelRepository engineModelRepository;

    public AddEngineModelToAircraftController(
            final AircraftModelRepository aircraftModelRepository,
            final EngineModelRepository engineModelRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
        this.engineModelRepository = engineModelRepository;
    }

    public List<AircraftModel> listAircraftModelsWithRemainingSlots() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final List<AircraftModel> available = new ArrayList<>();
        for (final AircraftModel model : aircraftModelRepository.findAll()) {
            if (model.supportedEngineModels().size() < model.numberOfEngines()) {
                available.add(model);
            }
        }
        return available;
    }

    public List<EngineModel> listAssignableEngineModels(final AircraftModel aircraftModel) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final List<EngineModel> available = new ArrayList<>();
        for (final EngineModel engineModel : engineModelRepository.findAll()) {
            if (aircraftModel.supportedEngineModels().stream()
                    .noneMatch(existing -> existing.name().equals(engineModel.name()))) {
                available.add(engineModel);
            }
        }
        return available;
    }

    public AircraftModel addEngineModelToAircraft(
            final String aircraftModelName,
            final String aircraftModelMaker,
            final String engineModelName) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        if (aircraftModelName == null || aircraftModelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Aircraft model name is required");
        }
        if (aircraftModelMaker == null || aircraftModelMaker.trim().isEmpty()) {
            throw new IllegalArgumentException("Aircraft model maker is required");
        }
        if (engineModelName == null || engineModelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Engine model name is required");
        }

        final List<AircraftModel> aircraftModels = aircraftModelRepository.findByModelNameAndMaker(
                aircraftModelName.trim(),
                aircraftModelMaker.trim());
        if (aircraftModels.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Aircraft model '%s' by manufacturer '%s' not found",
                    aircraftModelName,
                    aircraftModelMaker));
        }

        final AircraftModel aircraftModel = aircraftModels.get(0);
        if (aircraftModel.supportedEngineModels().size() >= aircraftModel.numberOfEngines()) {
            throw new IllegalArgumentException("This aircraft model already has all engine slots filled");
        }

        final EngineModel engineModel = engineModelRepository.findByName(engineModelName.trim())
                .orElseThrow(() -> new IllegalArgumentException("Engine model not found: " + engineModelName));

        if (aircraftModel.supportedEngineModels().stream()
                .anyMatch(existing -> existing.name().equals(engineModel.name()))) {
            throw new IllegalArgumentException("Engine model already associated with this aircraft model");
        }

        aircraftModel.addSupportedEngineModel(engineModel);
        return aircraftModelRepository.save(aircraftModel);
    }
}

