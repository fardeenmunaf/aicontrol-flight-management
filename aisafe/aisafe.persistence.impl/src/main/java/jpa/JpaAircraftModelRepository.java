package jpa;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.repositories.AircraftModelRepository;
import eapli.framework.domain.repositories.TransactionalContext;

import java.util.List;
import java.util.Map;

public class JpaAircraftModelRepository
        extends BaseJpaRepositoryBase<AircraftModel, Long, Long>
        implements AircraftModelRepository {

    public JpaAircraftModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaAircraftModelRepository(final String persistenceUnitName) {
        super("id");
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