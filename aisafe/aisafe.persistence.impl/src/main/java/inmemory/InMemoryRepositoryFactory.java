package inmemory;

import eapli.framework.domain.repositories.TransactionalContext;
import enginemodel.repositories.EngineModelRepository;
import user.repository.UserRepository;
import infrastructure.persistence.RepositoryFactory;
import flightPlan.repository.FlightPlanRepository;
import flightRoute.repository.FlightRouteRepository;
import node.repository.NodeRepository;
import flight.repository.FlightRepository;

/**
 * In-memory implementation of RepositoryFactory.
 * Returns in-memory repository instances for all aggregates.
 * Suitable for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemoryRepositoryFactory implements RepositoryFactory {

    @Override
    public TransactionalContext newTransactionalContext() {
        // in-memory does not support transactions
        return null;
    }

    @Override
    public UserRepository users(final TransactionalContext autoTx) {
        return new InMemoryUserRepository();  // in-memory repositories ignore transactional context
    }

    @Override
    public UserRepository users() {
        return users(null); // delegate to the method with transactional context, passing null since it's not supported
    }

    @Override
    public EngineModelRepository engineModels(final TransactionalContext autoTx) {
        return new InMemoryEngineModelRepository();
    }

    @Override
    public EngineModelRepository engineModels() {
        return engineModels(null);
    }

    @Override
    public FlightPlanRepository flightPlans(final TransactionalContext autoTx) {
        return new InMemoryFlightPlanRepository();
    }

    @Override
    public FlightPlanRepository flightPlans() {
        return flightPlans(null);
    }

    @Override
    public FlightRouteRepository flightRoutes(final TransactionalContext autoTx) {
        return new InMemoryFlightRouteRepository();
    }

    @Override
    public FlightRouteRepository flightRoutes() {
        return flightRoutes(null);
    }

    @Override
    public NodeRepository nodes(final TransactionalContext autoTx) {
        return new InMemoryNodeRepository();
    }

    @Override
    public NodeRepository nodes() {
        return nodes(null);
    }

    @Override
    public FlightRepository flights(final TransactionalContext autoTx) {
        return new InMemoryFlightRepository();
    }

    @Override
    public FlightRepository flights() {
        return flights(null);
    }
}
