package jpa;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.repositories.AircraftModelRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import shared.domain.AircraftModelId;

import java.util.List;
import java.util.Map;

public class JpaAircraftModelRepository
        extends BaseJpaRepositoryBase<AircraftModel, AircraftModelId, AircraftModelId>
        implements AircraftModelRepository {

    public JpaAircraftModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "aircraftModelId");
    }

    public JpaAircraftModelRepository(final String persistenceUnitName) {
        super("aircraftModelId");
    }

    @Override
    public List<AircraftModel> findByType(final AircraftType type) {
        return match("e.type = :type", Map.of("type", type));
    }

    @Override
    public List<AircraftModel> findByMakerName(final String makerName) {
        return match("LOWER(e.maker.name) = LOWER(:makerName)", Map.of("makerName", makerName));
    }
}