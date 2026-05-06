package aircraftmodel.repositories;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftType;
import eapli.framework.domain.repositories.DomainRepository;
import shared.domain.AircraftModelId;

import java.util.List;

public interface AircraftModelRepository extends DomainRepository<AircraftModelId, AircraftModel> {

    List<AircraftModel> findByType(AircraftType type);

    List<AircraftModel> findByMakerName(String makerName);

    List<AircraftModel> findByModelName(String modelName);

    List<AircraftModel> findByModelNameAndMaker(String modelName, String maker);

}