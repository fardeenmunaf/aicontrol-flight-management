package infrastructure.persistence;

import eapli.framework.domain.repositories.TransactionalContext;
import enginemodel.repositories.EngineModelRepository;
import user.repository.UserRepository;
import flightPlan.repository.FlightPlanRepository;
import flightRoute.repository.FlightRouteRepository;
import node.repository.NodeRepository;
import flight.repository.FlightRepository;
import simulationReport.repository.SimulationReportRepository;
import weatherData.repository.WeatherDataRepository;

/**
 * Master contract defining creation methods for all aggregate repositories.
 * Implementations are responsible for providing either JPA or In-Memory variants.
 *
 * @author LAPR4 Team
 */
public interface RepositoryFactory {

    /**
     * Factory method to create a transactional context for use in repositories.
     *
     * @return a new transactional context
     */
    TransactionalContext newTransactionalContext();

    /**
     * Get the EngineModel repository with auto-transaction management.
     *
     * @return the EngineModelRepository instance
     */
    EngineModelRepository engineModels();

    UserRepository users();

    FlightPlanRepository flightPlans();

    FlightRouteRepository flightRoutes();

    NodeRepository nodes();

    FlightRepository flights();

    /**
     * Get the EngineModel repository enrolled in a specific transactional context.
     *
     * @param autoTx the transactional context to enrol
     * @return the EngineModelRepository instance
     */
    EngineModelRepository engineModels(TransactionalContext autoTx);

    UserRepository users(TransactionalContext autoTx);

    FlightPlanRepository flightPlans(TransactionalContext autoTx);

    FlightRouteRepository flightRoutes(TransactionalContext autoTx);

    NodeRepository nodes(TransactionalContext autoTx);

    FlightRepository flights(TransactionalContext autoTx);

    SimulationReportRepository simulationReports();

    SimulationReportRepository simulationReports(TransactionalContext autoTx);

    WeatherDataRepository weatherData();

    WeatherDataRepository weatherData(TransactionalContext autoTx);
}
