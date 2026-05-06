package jpa;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import shared.domain.AirControlAreaId;

import java.util.List;
import java.util.Map;

public class JpaAirControlAreaRepository
        extends BaseJpaRepositoryBase<AirControlArea, AirControlAreaId, AirControlAreaId>
        implements AirControlAreaRepository {

    public JpaAirControlAreaRepository(final TransactionalContext autoTx) {
        super(autoTx, "airControlAreaId");
    }

    public JpaAirControlAreaRepository(final String persistenceUnitName) {
        super("airControlAreaId");
    }

    @Override
    public List<AirControlArea> findByAreaCode(final String areaCode) {
        return match("LOWER(e.areaCode) = LOWER(:areaCode)", Map.of("areaCode", areaCode));
    }

    @Override
    public List<AirControlArea> findByMinimumRequiredArrivalFuelGreaterThanEqual(final double minFuel) {
        return match("e.minimumRequiredArrivalFuel >= :minFuel", Map.of("minFuel", minFuel));
    }

    @Override
    public List<AirControlArea> findByGeographicBoundaries(final String geographicBoundaries) {
        return match("LOWER(e.geographicBoundaries) = LOWER(:geographicBoundaries)",
                Map.of("geographicBoundaries", geographicBoundaries));
    }
}
