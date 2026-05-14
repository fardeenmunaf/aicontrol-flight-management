package aircraftmodel.application;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.dto.AircraftModelDTO;
import aircraftmodel.repositories.AircraftModelRepository;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ViewAircraftModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepository;

    public ViewAircraftModelController(
            final AircraftModelRepository aircraftModelRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
    }

    public List<AircraftModelDTO> listAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final Iterable<AircraftModel> models = aircraftModelRepository.findAll();

        final List<AircraftModelDTO> dtos = new ArrayList<>();

        for (final AircraftModel model : models) {

            final List<String> engineNames = model.supportedEngineModels()
                    .stream()
                    .map(engine -> engine.name())
                    .collect(Collectors.toList());

            dtos.add(new AircraftModelDTO(
                    model.modelName(),
                    model.maker().name(),
                    model.maker().country(),
                    model.type(),
                    model.emptyWeight(),
                    model.maximumTakeOffWeight(),
                    model.maximumZeroFuelWeight(),
                    model.maximumFuelCapacity(),
                    model.serviceCeiling(),
                    model.cruiseSpeed(),
                    model.wingArea(),
                    model.wingSpan(),
                    model.dragCoefficient(),
                    model.liftCoefficient(),
                    model.numberOfEngines(),
                    engineNames
            ));
        }

        return dtos;
    }
}