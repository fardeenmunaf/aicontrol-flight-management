package aircontrolarea.repositories;

import eapli.framework.domain.repositories.DomainRepository;
import aircontrolarea.domain.AirControlArea;
import shared.domain.AirControlAreaId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AirControlArea aggregate.
 * Defines domain-specific persistence operations for AirControlArea entities.
 */
public interface AirControlAreaRepository extends DomainRepository<AirControlAreaId, AirControlArea> {

    /**
     * Returns all control areas matching a given area code.
     *
     * @param areaCode the area code to filter by
     * @return list of control areas with the given area code, never null
     */
    List<AirControlArea> findByAreaCode(String areaCode);

    /**
     * Returns all control areas with a minimum required arrival fuel greater than or equal to the given value.
     *
     * @param minFuel minimum required arrival fuel
     * @return list of control areas matching the criteria, never null
     */
    List<AirControlArea> findByMinimumRequiredArrivalFuelGreaterThanEqual(double minFuel);

    /**
     * Returns all control areas matching a given geographic boundaries string.
     *
     * @param geographicBoundaries the geographic boundaries to filter by
     * @return list of control areas with the given boundaries, never null
     */
    List<AirControlArea> findByGeographicBoundaries(String geographicBoundaries);

    /**
     * Returns the first air control area whose polygon contains the given point.
     * Uses the "lat,lon;lat,lon;..." boundary format and a ray-casting algorithm.
     *
     * @param latitude  point latitude
     * @param longitude point longitude
     * @return the containing area, or empty if none found
     */
    default Optional<AirControlArea> findContaining(final double latitude, final double longitude) {
        for (final AirControlArea area : findAll()) {
            if (area.containsPoint(latitude, longitude)) {
                return Optional.of(area);
            }
        }
        return Optional.empty();
    }
}