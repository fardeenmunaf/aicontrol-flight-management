package aircraft.repositories;

import eapli.framework.domain.repositories.DomainRepository;
import aircraft.domain.Aircraft;
import aircraft.domain.CabinConfiguration;
import shared.domain.AircraftId;
import shared.domain.AirTransportCompanyId;
import shared.domain.AircraftModelId;

import java.util.List;

/**
 * Repository interface for Aircraft aggregate.
 * Defines domain-specific persistence operations for Aircraft entities.
 *
 * @author LAPR4 Team
 */

public interface AircraftRepository extends DomainRepository<AircraftId, Aircraft> {

    /**
     * Returns all aircraft matching a given cabin configuration.
     * Useful for finding all aircraft available for a specific flight plan
     * based on required cabin configuration.
     *
     * @param cabinConfiguration the cabin configuration to filter by
     * @return list of aircraft with the given cabin configuration, never null
     */
    List<Aircraft> findByCabinConfiguration(CabinConfiguration cabinConfiguration);

    /**
     * Returns all aircraft matching a given operation status.
     * Used to find all active aircraft available for flight plan assignment.
     *
     * @param operationStatus the operation status to filter by
     * @return list of aircraft with the given operation status, never null
     */
    List<Aircraft> findByOperationStatus(String operationStatus);

    /**
     * Returns all aircraft matching a given registration country.
     * Useful for finding all aircraft registered in a specific country,
     * which may be relevant for regulatory compliance checks.
     *
     * @param registrationCountry the registration country to filter by
     * @return list of aircraft with the given registration country, never null
     */
    List<Aircraft> findByRegistrationCountry(String registrationCountry);

    /**
     * Find an aircraft by its registration number (case-insensitive).
     *
     * @param registrationNumber registration number to search for
     * @return optional containing the aircraft if present
     */
    java.util.Optional<Aircraft> findByRegistrationNumber(String registrationNumber);

    /**
     * Returns all aircraft belonging to a given air transport company.
     * Includes aircraft regardless of their operational status.
     *
     * @param companyId the air transport company ID to filter by
     * @return list of aircraft owned by the company, never null
     */
    List<Aircraft> findByAirTransportCompanyId(AirTransportCompanyId companyId);

    boolean existsByAircraftModel(AircraftModelId aircraftModelId);
}