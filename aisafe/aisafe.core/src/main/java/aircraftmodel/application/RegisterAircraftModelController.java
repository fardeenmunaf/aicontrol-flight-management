package aircraftmodel.aplication;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
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

        if (dto.engineModelNames == null || dto.engineModelNames.isEmpty()) {
            throw new IllegalArgumentException("At least one certified engine model must be associated");
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
                new AircraftModelMaker(dto.makerName, dto.makerCountry)
        );

        final Set<String> uniqueEngineNames = new LinkedHashSet<>();
        for (final String name : dto.engineModelNames) {
            if (name != null && !name.trim().isEmpty()) {
                uniqueEngineNames.add(name.trim());
            }
        }

        if (uniqueEngineNames.isEmpty()) {
            throw new IllegalArgumentException("At least one certified engine model must be associated");
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

        for (final String engineName : uniqueEngineNames) {
            final var engine = engineModelRepository.findByName(engineName)
                    .orElseThrow(() -> new IllegalArgumentException("Engine model not found: " + engineName));
            model.addSupportedEngineModel(engine);
        }

        return aircraftModelRepository.save(model);
    }
}

