package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import simulation.domain.Simulation;
import simulation.domain.SimulationStatus;
import simulation.repository.SimulationRepository;
import flight.domain.Flight;
import shared.domain.SimulationId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaSimulationRepository extends BaseJpaRepositoryBase<Simulation, SimulationId, SimulationId>
		implements SimulationRepository {

	public JpaSimulationRepository(final TransactionalContext autoTx) {
		super(autoTx, "simulationId");
	}

	public JpaSimulationRepository(final String persistenceUnitName) {
		super("simulationId");
	}

	@Override
	public Optional<Simulation> findActive() {
		return matchOne("e.status = :status", Map.of("status", SimulationStatus.RUNNING));
	}

	@Override
	public List<Simulation> findByStatus(final SimulationStatus status) {
		return match("e.status = :status", Map.of("status", status));
	}

	@Override
	public List<Simulation> findByTrackedFlight(final Flight flight) {
		return match("?1 MEMBER OF e.trackedFlights", Map.of(1, flight));
	}

	@Override
	public List<Simulation> findByStartDateTimeBetween(final java.time.Instant from, final java.time.Instant to) {
		return match("e.startDateTime >= :from AND e.startDateTime < :to",
				Map.of("from", from, "to", to));
	}
}
