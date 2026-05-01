package simulationReport.repository;

import eapli.framework.domain.repositories.DomainRepository;
import simulationReport.domain.SimulationReport;

/**
 * Repository interface for SimulationReport aggregate.
 * Defines domain-specific persistence operations for SimulationReport entities.
 *
 * @author LAPR4 Team
 */
public interface SimulationReportRepository extends DomainRepository<Long, SimulationReport> {
    // Domain-specific query methods can be added here as needed
}

