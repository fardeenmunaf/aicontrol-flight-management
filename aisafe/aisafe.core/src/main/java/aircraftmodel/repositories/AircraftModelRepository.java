package aircraftmodel.repositories;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftType;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.List;

public interface AircraftModelRepository extends DomainRepository<Long, AircraftModel> {

    List<AircraftModel> findByType(AircraftType type);

    List<AircraftModel> findByMakerName(String makerName);
}