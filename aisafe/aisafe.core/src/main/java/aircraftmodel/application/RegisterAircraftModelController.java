package aircraftmodel.application;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.dto.AircraftModelDTO;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.repositories.AircraftModelRepository;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import enginemodel.repositories.EngineModelRepository;

import java.util.LinkedHashSet;
import java.util.Set;

public class RegisterAircraftModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepository;
    private final EngineModelRepository engineModelRepository;

    public RegisterAircraftModelController(
            final AircraftModelRepository aircraftModelRepository,
            final EngineModelRepository engineModelRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
        this.engineModelRepository = engineModelRepository;
    }

    public AircraftModel registerAircraftModel(final AircraftModelDTO dto) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        if (dto == null) {
            throw new IllegalArgumentException("Aircraft model data is required");
        }

        if (dto.numberOfEngines < 0) {
            throw new IllegalArgumentException("Number of engines cannot be negative");
        }

        if (dto.numberOfEngines == 0) {
            if (dto.engineModelNames != null && !dto.engineModelNames.isEmpty()) {
                throw new IllegalArgumentException("No engine models must be selected when number of engines is zero");
            }
        } else {
            if (dto.engineModelNames == null || dto.engineModelNames.size() != dto.numberOfEngines) {
                throw new IllegalArgumentException(String.format(
                        "Select exactly %d engine model(s) for this aircraft model",
                        dto.numberOfEngines));
            }
        }

        if (!aircraftModelRepository.findByModelNameAndMaker(dto.modelName, dto.makerName).isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Aircraft model '%s' by manufacturer '%s' already exists",
                    dto.modelName,
                    dto.makerName
            ));
        }

        final AircraftModel model = new AircraftModel(
                dto.type,
                dto.emptyWeight,
                dto.maximumTakeOffWeight,
                dto.maximumZeroFuelWeight,
                dto.maximumFuelCapacity,
                dto.serviceCeiling,
                dto.cruiseSpeed,
                dto.wingArea,
                dto.wingSpan,
                dto.dragCoefficient,
                dto.liftCoefficient,
                dto.modelName,
                dto.numberOfEngines,
                new AircraftModelMaker(dto.makerName, dto.makerCountry)
        );

        final Set<String> uniqueEngineNames = new LinkedHashSet<>();
        for (final String name : dto.engineModelNames) {
            if (name != null && !name.trim().isEmpty()) {
                uniqueEngineNames.add(name.trim());
            }
        }

        if (dto.emptyWeight <= 0 ||
                dto.maximumTakeOffWeight <= 0 ||
                dto.maximumZeroFuelWeight <= 0 ||
                dto.maximumFuelCapacity <= 0 ||
                dto.serviceCeiling <= 0 ||
                dto.cruiseSpeed <= 0 ||
                dto.wingArea <= 0 ||
                dto.wingSpan <= 0 ||
                dto.dragCoefficient <= 0 ||
                dto.liftCoefficient <= 0) {
            throw new IllegalArgumentException("All flight characteristics must be positive");
        }

        if (dto.numberOfEngines > 0 && uniqueEngineNames.isEmpty()) {
            throw new IllegalArgumentException("At least one certified engine model must be associated");
        }

        for (final String engineName : uniqueEngineNames) {
            final var engine = engineModelRepository.findByName(engineName)
                    .orElseThrow(() -> new IllegalArgumentException("Engine model not found: " + engineName));
            model.addSupportedEngineModel(engine);
        }

        return aircraftModelRepository.save(model);
    }
}

