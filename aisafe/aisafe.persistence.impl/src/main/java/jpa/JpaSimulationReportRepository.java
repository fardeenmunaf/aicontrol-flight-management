package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import simulationReport.domain.SimulationReport;
import simulationReport.repository.SimulationReportRepository;

/**
 * JPA implementation of SimulationReportRepository.
 * Provides ORM-based persistence for SimulationReport aggregates.
 *
 * @author LAPR4 Team
 */
class JpaSimulationReportRepository
        extends BaseJpaRepositoryBase<SimulationReport, Long, Long>
        implements SimulationReportRepository {

    public JpaSimulationReportRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaSimulationReportRepository(final String persistenceUnitName) {
        super("id");
    }
}

