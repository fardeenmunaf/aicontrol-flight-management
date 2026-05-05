package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import simulationReport.domain.SimulationReport;
import simulationReport.repository.SimulationReportRepository;
import shared.domain.SimulationReportId;

/**
 * In-memory implementation of SimulationReportRepository.
 * Used for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemorySimulationReportRepository extends InMemoryDomainRepository<SimulationReport, SimulationReportId>
        implements SimulationReportRepository {

    static {
        InMemoryInitializer.init();
    }
}

