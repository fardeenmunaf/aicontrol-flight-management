package bootstrap;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.repositories.AircraftModelRepository;
import enginemodel.repositories.EngineModelRepository;

public class AircraftModelBootstrap {

    private final AircraftModelRepository aircraftModelRepository;
    private final EngineModelRepository engineModelRepository;

    public AircraftModelBootstrap(final AircraftModelRepository aircraftModelRepository,
                                  final EngineModelRepository engineModelRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
        this.engineModelRepository = engineModelRepository;
    }

    public void execute() {
        registerIfAbsent(
                "B737-800", "Boeing", "USA",
                AircraftType.PASSENGER,
                41413.0, 79015.0, 62730.0, 26020.0, 12500.0, 840.0,
                124.6, 35.8, 0.024, 1.25, 2,
                "CFM56-7B", "CFM56-7B27"
        );

        registerIfAbsent(
                "ATR 72-600", "ATR", "France",
                AircraftType.PASSENGER,
                13500.0, 23000.0, 20800.0, 5000.0, 7600.0, 510.0,
                61.0, 27.1, 0.030, 1.15, 2,
                "PW127M", "PW127F"
        );

        registerIfAbsent(
                "B777-300ER", "Boeing", "USA",
                AircraftType.PASSENGER,
                167829.0, 351534.0, 237682.0, 181283.0, 13100.0, 905.0,
                436.8, 64.8, 0.022, 1.30, 2,
                "GE90-115B", "GE90-110B"
        );
    }

    private void registerIfAbsent(final String modelName,
                                  final String makerName,
                                  final String makerCountry,
                                  final AircraftType type,
                                  final Double emptyWeight,
                                  final Double maximumTakeOffWeight,
                                  final Double maximumZeroFuelWeight,
                                  final Double maximumFuelCapacity,
                                  final Double serviceCeiling,
                                  final Double cruiseSpeed,
                                  final Double wingArea,
                                  final Double wingSpan,
                                  final Double dragCoefficient,
                                  final Double liftCoefficient,
                                  final int numberOfEngines,
                                  final String engineModelName1,
                                  final String engineModelName2) {

        if (!aircraftModelRepository.findByModelNameAndMaker(modelName, makerName).isEmpty()) {
            return;
        }

        final var model = new AircraftModel(
                type, emptyWeight, maximumTakeOffWeight, maximumZeroFuelWeight,
                maximumFuelCapacity, serviceCeiling, cruiseSpeed, wingArea,
                wingSpan, dragCoefficient, liftCoefficient, modelName,
                numberOfEngines, new AircraftModelMaker(makerName, makerCountry)
        );

        engineModelRepository.findByName(engineModelName1).ifPresent(model::addSupportedEngineModel);
        engineModelRepository.findByName(engineModelName2).ifPresent(model::addSupportedEngineModel);

        aircraftModelRepository.save(model);
    }
}