package bootstrap;

import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineModelMaker;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelProperties;
import enginemodel.domain.FuelType;
import enginemodel.repositories.EngineModelRepository;

public class EngineModelBootstrap {

	private final EngineModelRepository engineModelRepository;

	public EngineModelBootstrap(final EngineModelRepository engineModelRepository) {
		this.engineModelRepository = engineModelRepository;
	}

	public void execute() {
		registerIfAbsent("CFM56-7B", "CFM International", "France", EngineType.TURBOFAN,
				121.0, 0.38, 121000.0, 98000.0, 0.62,
				FuelType.JET_A, 0.80, 43.0);

		registerIfAbsent("PW127M", "Pratt & Whitney Canada", "Canada", EngineType.TURBOPROP,
				2.05, 0.35, 20500.0, 16200.0, 0.58,
				FuelType.JET_A, 0.80, 43.0);

		registerIfAbsent("GE90-115B", "General Electric", "USA", EngineType.TURBOFAN,
				513.0, 0.40, 513000.0, 430000.0, 0.60,
				FuelType.JET_A, 0.80, 43.0);
	}

	private void registerIfAbsent(final String name,
								  final String makerName,
								  final String makerCountry,
								  final EngineType type,
								  final Double power,
								  final Double efficiency,
								  final Double thrustSeaLevelStopped,
								  final Double thrustSeaLevelCruise,
								  final Double thrustSpecificFuelConsumption,
								  final FuelType fuelType,
								  final Double fuelDensity,
								  final Double fuelSpecificEnergy) 
    {
		if (engineModelRepository.findByNameAndMakerName(name, makerName).isPresent()) 
        {
			return;
		}

		final var maker = new EngineModelMaker(makerName, makerCountry);
		final var fuelProperties = new FuelProperties(fuelType, fuelDensity, fuelSpecificEnergy);

		final var engineModel = new EngineModel(
				name,
				type,
				power,
				efficiency,
				thrustSeaLevelStopped,
				thrustSeaLevelCruise,
				thrustSpecificFuelConsumption,
				maker,
				fuelProperties
		);

		engineModelRepository.save(engineModel);
	}
}
