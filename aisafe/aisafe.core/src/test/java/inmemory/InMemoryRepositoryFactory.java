package inmemory;

import eapli.framework.domain.repositories.TransactionalContext;

/**
 * Test-scoped InMemoryRepositoryFactory implementing the production
 * RepositoryFactory interface. All methods return null except
 * weatherData(...) which returns the test InMemoryWeatherDataRepository.
 *
 * This class exists only in test sources so PersistenceContext can
 * Class.forName("inmemory.InMemoryRepositoryFactory") during tests.
 */
public class InMemoryRepositoryFactory implements infrastructure.persistence.RepositoryFactory {

    public InMemoryRepositoryFactory() {
        // public no-arg constructor required for reflection
    }

    @Override
    public TransactionalContext newTransactionalContext() {
        return null;
    }

    @Override
    public enginemodel.repositories.EngineModelRepository engineModels() {
        return null;
    }

    @Override
    public user.repository.UserRepository users() {
        return null;
    }

    @Override
    public flightPlan.repository.FlightPlanRepository flightPlans() {
        return null;
    }

    @Override
    public flightRoute.repository.FlightRouteRepository flightRoutes() {
        return null;
    }

    @Override
    public node.repository.NodeRepository nodes() {
        return null;
    }

    @Override
    public flight.repository.FlightRepository flights() {
        return null;
    }

    @Override
    public pilot.repository.PilotRepository pilots() {
        return null;
    }

    @Override
    public simulation.repository.SimulationRepository simulations() {
        return null;
    }

    @Override
    public enginemodel.repositories.EngineModelRepository engineModels(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public user.repository.UserRepository users(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public flightPlan.repository.FlightPlanRepository flightPlans(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public flightRoute.repository.FlightRouteRepository flightRoutes(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public node.repository.NodeRepository nodes(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public flight.repository.FlightRepository flights(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public simulationReport.repository.SimulationReportRepository simulationReports() {
        return null;
    }

    @Override
    public simulationReport.repository.SimulationReportRepository simulationReports(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public weatherData.repository.WeatherDataRepository weatherData() {
        return new InMemoryWeatherDataRepository();
    }

    @Override
    public weatherData.repository.WeatherDataRepository weatherData(final TransactionalContext autoTx) {
        return weatherData();
    }

    @Override
    public aircraftmodel.repositories.AircraftModelRepository aircraftModels() {
        return null;
    }

    @Override
    public aircraftmodel.repositories.AircraftModelRepository aircraftModels(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public aircraft.repositories.AircraftRepository aircraft() {
        return null;
    }

    @Override
    public aircraft.repositories.AircraftRepository aircraft(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public aircontrolarea.repositories.AirControlAreaRepository airControlAreas() {
        return null;
    }

    @Override
    public airtransportcompany.repositories.AirTransportCompanyRepository airTransportCompanies(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public airtransportcompany.repositories.AirTransportCompanyRepository airTransportCompanies() {
        return null;
    }

    @Override
    public aircontrolarea.repositories.AirControlAreaRepository airControlAreas(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public airport.repositories.AirportRepository airports() {
        return null;
    }

    @Override
    public airport.repositories.AirportRepository airports(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public pilot.repository.PilotRepository pilots(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public simulation.repository.SimulationRepository simulations(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public customercollaborator.repositories.CustomerCollaboratorRepository customerCollaborators() {
        return null;
    }

    @Override
    public customercollaborator.repositories.CustomerCollaboratorRepository customerCollaborators(final TransactionalContext autoTx) {
        return null;
    }

    @Override
    public pilot.repository.PilotCompanyAssignmentRepository pilotCompanyAssignments() {
        return null;
    }

    @Override
    public pilot.repository.PilotCompanyAssignmentRepository pilotCompanyAssignments(final TransactionalContext autoTx) {
        return null;
    }
}


