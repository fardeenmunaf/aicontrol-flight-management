package flight.repository;

import eapli.framework.domain.repositories.DomainRepository;
import flight.domain.Flight;
import flight.domain.FlightStatus;
import aircontrolarea.domain.AirControlArea;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Flight aggregate.
 * Defines domain-specific persistence operations for Flight entities.
 *
 * @author LAPR4 Team
 */
public interface FlightRepository extends DomainRepository<Long, Flight> {

    /**
     * Returns all flights currently active within a given air control area.
     * Used by the simulation engine to determine which flights need
     * position updates and conflict checks each tick.
     *
     * @param areaId the identity of the AirControlArea to query
     * @return list of flights currently inside the given area, never null
     */
    List<Flight> findActiveFlightsInArea(AirControlArea area);

    /**
     * Returns all flights matching a given status.
     * Useful for querying flights that are scheduled, airborne, or completed.
     *
     * @param status the flight status to filter by
     * @return list of flights with the given status, never null
     */
    List<Flight> findByStatus(FlightStatus status);

    /**
     * Returns the flight associated with a given flight designator, if any.
     * Flight designators are unique identifiers used by air traffic control.
     *
     * @param flightDesignator the flight designator (e.g. "TAP123")
     * @return an Optional containing the flight, or empty if not found
     */
    Optional<Flight> findByFlightDesignator(String flightDesignator);
}