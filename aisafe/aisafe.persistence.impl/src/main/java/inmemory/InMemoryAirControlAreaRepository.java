package inmemory;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import shared.domain.AirControlAreaId;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAirControlAreaRepository
        extends InMemoryDomainRepository<AirControlArea, AirControlAreaId>
        implements AirControlAreaRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public List<AirControlArea> findByAreaCode(final String areaCode) {
        final List<AirControlArea> result = new ArrayList<>();
        match(a -> a.areaCode().equalsIgnoreCase(areaCode)).forEach(result::add);
        return result;
    }

    @Override
    public List<AirControlArea> findByMinimumRequiredArrivalFuelGreaterThanEqual(final double minFuel) {
        final List<AirControlArea> result = new ArrayList<>();
        match(a -> a.minimumRequiredArrivalFuel() >= minFuel).forEach(result::add);
        return result;
    }

    @Override
    public List<AirControlArea> findByGeographicBoundaries(final String geographicBoundaries) {
        final List<AirControlArea> result = new ArrayList<>();
        match(a -> a.geographicBoundaries().equalsIgnoreCase(geographicBoundaries)).forEach(result::add);
        return result;
    }
}

