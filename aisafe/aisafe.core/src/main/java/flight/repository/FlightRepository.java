package flight.repository;

import eapli.framework.domain.repositories.DomainRepository;
import flight.domain.Flight;
import flight.domain.FlightStatus;
import shared.domain.FlightId;

import java.util.List;
import java.util.Optional;
import shared.domain.AircraftId;

/**
 * Repository interface for Flight aggregate.
 * Defines domain-specific persistence operations for Flight entities.
 *
 * @author LAPR4 Team
 */
public interface FlightRepository extends DomainRepository<FlightId, Flight> {

    /**
     * Returns all flights currently airborne and scheduled to land at a given airport.
     * Used by the simulation engine to anticipate incoming traffic at an airport.
     *
     * @param iataCode the IATA code of the destination airport
     * @return list of active flights landing at the given airport, never null
     */
    List<Flight> findActiveFlightsLandingInAirport(final String iataCode);

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

    /**
     * Returns all flights assigned to the given aircraft that match the provided status.
     * Useful to determine pending flights for a specific aircraft.
     */
    List<Flight> findByAircraftAndStatus(AircraftId aircraftId, FlightStatus status);
}