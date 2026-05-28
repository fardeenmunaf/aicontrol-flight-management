package bootstrap;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.domain.CabinConfiguration;
import aircraft.repositories.AircraftRepository;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.repositories.AircraftModelRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import enginemodel.domain.EngineModel;
import enginemodel.repositories.EngineModelRepository;

import java.util.List;
import java.util.Optional;

public class AircraftBootstrap {

    private final AircraftRepository aircraftRepository;
    private final AircraftModelRepository aircraftModelRepository;
    private final EngineModelRepository engineModelRepository;
    private final AirTransportCompanyRepository airTransportCompanyRepository;

    public AircraftBootstrap(final AircraftRepository aircraftRepository,
                             final AircraftModelRepository aircraftModelRepository,
                             final EngineModelRepository engineModelRepository,
                             final AirTransportCompanyRepository airTransportCompanyRepository) {
        this.aircraftRepository = aircraftRepository;
        this.aircraftModelRepository = aircraftModelRepository;
        this.engineModelRepository = engineModelRepository;
        this.airTransportCompanyRepository = airTransportCompanyRepository;
    }

    public void execute() {
        final List<AirTransportCompany> tap = airTransportCompanyRepository.findByIataCode("TP");
        if (tap.isEmpty()) {
            System.out.println("TAP Air Portugal not found — skipping aircraft bootstrap.");
            return;
        }
        final AirTransportCompany tapCompany = tap.get(0);

        final Optional<AircraftModel> model777 = firstModel("777-200ER", "Boeing");
        final Optional<EngineModel> engineGE = engineModelRepository.findByName("GE 90B");

        final Optional<AircraftModel> modelA380 = firstModel("A380", "Airbus");
        final Optional<EngineModel> engineTrent = engineModelRepository.findByName("RR Trent 970");

        if (model777.isPresent() && engineGE.isPresent()) {
            registerIfAbsent("CS-TUA", "Portugal", 2, model777.get(), engineGE.get(),
                    new CabinConfiguration(264, 8, 48), tapCompany);
            registerIfAbsent("CS-TUB", "Portugal", 2, model777.get(), engineGE.get(),
                    new CabinConfiguration(264, 8, 48), tapCompany);
        } else {
            System.out.println("777-200ER or GE 90B not found — skipping 777 aircraft.");
        }

        if (modelA380.isPresent() && engineTrent.isPresent()) {
            registerIfAbsent("CS-TKQ", "Portugal", 2, modelA380.get(), engineTrent.get(),
                    new CabinConfiguration(399, 14, 80), tapCompany);
        } else {
            System.out.println("A380 or RR Trent 970 not found — skipping A380 aircraft.");
        }
    }

    private Optional<AircraftModel> firstModel(final String modelName, final String makerName) {
        final List<AircraftModel> found = aircraftModelRepository.findByModelNameAndMaker(modelName, makerName);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    private void registerIfAbsent(final String reg,
                                   final String country,
                                   final int crew,
                                   final AircraftModel model,
                                   final EngineModel engine,
                                   final CabinConfiguration cabin,
                                   final AirTransportCompany company) {
        if (aircraftRepository.findByRegistrationNumber(reg).isPresent()) {
            return;
        }
        final Aircraft aircraft = new Aircraft(reg, country, AircraftOperationStatus.IN_SERVICE,
                crew, model, engine, cabin, company);
        aircraftRepository.save(aircraft);
        System.out.println("Aircraft bootstrapped: " + reg + " (" + model.modelName() + ")");
    }
}
