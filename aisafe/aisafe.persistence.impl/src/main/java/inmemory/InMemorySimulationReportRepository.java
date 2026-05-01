package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import simulationReport.domain.SimulationReport;
import simulationReport.repository.SimulationReportRepository;

/**
 * In-memory implementation of SimulationReportRepository.
 * Used for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemorySimulationReportRepository extends InMemoryDomainRepository<SimulationReport, Long>
        implements SimulationReportRepository {

    static {
        InMemoryInitializer.init();
    }
}

