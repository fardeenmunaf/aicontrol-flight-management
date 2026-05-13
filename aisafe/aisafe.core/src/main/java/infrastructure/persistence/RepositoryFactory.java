package infrastructure.persistence;

import aircraft.repositories.AircraftRepository;
import aircraftmodel.repositories.AircraftModelRepository;
import airport.repositories.AirportRepository;
import aircontrolarea.repositories.AirControlAreaRepository;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import enginemodel.repositories.EngineModelRepository;
import user.repository.UserRepository;
import flightPlan.repository.FlightPlanRepository;
import flightRoute.repository.FlightRouteRepository;
import node.repository.NodeRepository;
import flight.repository.FlightRepository;
import simulationReport.repository.SimulationReportRepository;
import weatherData.repository.WeatherDataRepository;
import pilot.repository.PilotRepository;
import simulation.repository.SimulationRepository;

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

    EngineModelRepository engineModels();

    UserRepository users();

    FlightPlanRepository flightPlans();

    FlightRouteRepository flightRoutes();

    NodeRepository nodes();

    FlightRepository flights();

    PilotRepository pilots();

    SimulationRepository simulations();


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

    AircraftModelRepository aircraftModels();

    AircraftModelRepository aircraftModels(TransactionalContext autoTx);

    AircraftRepository aircraft();

    AircraftRepository aircraft(TransactionalContext autoTx);

    /**
     * Air control area repositories
     */
    AirControlAreaRepository airControlAreas();

    airtransportcompany.repositories.AirTransportCompanyRepository airTransportCompanies(TransactionalContext autoTx);

    airtransportcompany.repositories.AirTransportCompanyRepository airTransportCompanies();

    AirControlAreaRepository airControlAreas(TransactionalContext autoTx);

    AirportRepository airports();

    AirportRepository airports(TransactionalContext autoTx);

    PilotRepository pilots(TransactionalContext autoTx);

    SimulationRepository simulations(TransactionalContext autoTx);

    CustomerCollaboratorRepository customerCollaborators();

    CustomerCollaboratorRepository customerCollaborators(TransactionalContext autoTx);
}
