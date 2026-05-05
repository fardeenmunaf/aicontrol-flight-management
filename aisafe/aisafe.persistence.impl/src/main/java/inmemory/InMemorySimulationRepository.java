package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import simulation.domain.Simulation;
import simulation.domain.SimulationStatus;
import simulation.repository.SimulationRepository;
import flight.domain.Flight;
import shared.domain.SimulationId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemorySimulationRepository extends InMemoryDomainRepository<Simulation, SimulationId>
		implements SimulationRepository {

	static {
		InMemoryInitializer.init();
	}

	@Override
	public Optional<Simulation> findActive() {
		return matchOne(s -> s.status() == SimulationStatus.RUNNING);
	}

	@Override
	public List<Simulation> findByStatus(final SimulationStatus status) {
		final List<Simulation> result = new ArrayList<>();
		match(s -> s.status() == status).forEach(result::add);
		return result;
	}

	@Override
	public List<Simulation> findByTrackedFlight(final Flight flight) {
		final List<Simulation> result = new ArrayList<>();
		match(s -> s.trackedFlights().contains(flight)).forEach(result::add);
		return result;
	}
}
