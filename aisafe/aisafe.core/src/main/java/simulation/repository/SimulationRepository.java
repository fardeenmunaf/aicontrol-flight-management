package simulation.repository;

import eapli.framework.domain.repositories.DomainRepository;
import simulation.domain.Simulation;
import simulation.domain.SimulationStatus;
import flight.domain.Flight;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Simulation aggregate.
 * Defines domain-specific persistence operations for Simulation entities.
 *
 * @author LAPR4 Team
 */
public interface SimulationRepository extends DomainRepository<Long, Simulation> {

    /**
     * Returns the currently running simulation, if any.
     * At most one simulation should be RUNNING at a given time.
     *
     * @return an Optional containing the active simulation, or empty if none is running
     */
    Optional<Simulation> findActive();

    /**
     * Returns all simulations matching a given status.
     * Useful for listing completed simulations for reporting, or
     * finding created ones awaiting execution.
     *
     * @param status the simulation status to filter by
     * @return list of simulations with the given status, never null
     */
    List<Simulation> findByStatus(SimulationStatus status);

    /**
     * Returns all simulations that track a given flight.
     * Useful for tracing which simulation runs involved a specific flight.
     *
     * @param flight the flight to search for
     * @return list of simulations tracking the given flight, never null
     */
    List<Simulation> findByTrackedFlight(Flight flight);
}