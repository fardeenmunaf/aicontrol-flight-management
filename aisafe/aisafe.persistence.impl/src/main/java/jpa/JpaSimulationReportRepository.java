package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import simulationReport.domain.SimulationReport;
import simulationReport.repository.SimulationReportRepository;
import shared.domain.SimulationReportId;

/**
 * JPA implementation of SimulationReportRepository.
 * Provides ORM-based persistence for SimulationReport aggregates.
 *
 * @author LAPR4 Team
 */
class JpaSimulationReportRepository
                extends BaseJpaRepositoryBase<SimulationReport, SimulationReportId, SimulationReportId>
        implements SimulationReportRepository {

    public JpaSimulationReportRepository(final TransactionalContext autoTx) {
		super(autoTx, "simulationReportId");
    }

    public JpaSimulationReportRepository(final String persistenceUnitName) {
		super("simulationReportId");
    }
}

