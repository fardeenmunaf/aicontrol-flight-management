package jpa;

import aircraft.repositories.AircraftRepository;
import aircraftmodel.repositories.AircraftModelRepository;
import airport.repositories.AirportRepository;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import user.repository.UserRepository;
import enginemodel.repositories.EngineModelRepository;
import infrastructure.persistence.RepositoryFactory;
import flightPlan.repository.FlightPlanRepository;
import flightRoute.repository.FlightRouteRepository;
import node.repository.NodeRepository;
import flight.repository.FlightRepository;
import simulationReport.repository.SimulationReportRepository;
import weatherData.repository.WeatherDataRepository;
import pilot.repository.PilotRepository;
import simulation.repository.SimulationRepository;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.repositories.AirTransportCompanyRepository;


/**
 * JPA implementation of RepositoryFactory.
 * Returns JPA-backed repository instances for all aggregates.
 * Suitable for production and integration testing environments.
 * Manages transactional contexts for auto-transaction management.
 *
 * @author LAPR4 Team
 */
public class JpaRepositoryFactory implements RepositoryFactory {

    @Override
    public TransactionalContext newTransactionalContext() {
        return JpaAutoTxRepository.buildTransactionalContext(PersistenceSettings.getPersistenceUnitName(), PersistenceSettings.getExtendedPersistenceProperties());
    }

    @Override
    public UserRepository users() {
        return new JpaUserRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public UserRepository users(final TransactionalContext autoTx) {
        return new JpaUserRepository(autoTx);
    }

    @Override
    public EngineModelRepository engineModels() {
        return new JpaEngineModelRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public EngineModelRepository engineModels(final TransactionalContext autoTx) {
        return new JpaEngineModelRepository(autoTx);
    }

    @Override
    public FlightPlanRepository flightPlans() {
        return new JpaFlightPlanRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public FlightPlanRepository flightPlans(final TransactionalContext autoTx) {
        return new JpaFlightPlanRepository(autoTx);
    }

    @Override
    public FlightRouteRepository flightRoutes() {
        return new JpaFlightRouteRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public FlightRouteRepository flightRoutes(final TransactionalContext autoTx) {
        return new JpaFlightRouteRepository(autoTx);
    }

    @Override
    public NodeRepository nodes() {
        return new JpaNodeRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public NodeRepository nodes(final TransactionalContext autoTx) {
        return new JpaNodeRepository(autoTx);
    }

    @Override
    public FlightRepository flights() {
        return new JpaFlightRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public FlightRepository flights(final TransactionalContext autoTx) {
        return new JpaFlightRepository(autoTx);
    }

    @Override
    public SimulationReportRepository simulationReports() {
        return new JpaSimulationReportRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public SimulationReportRepository simulationReports(final TransactionalContext autoTx) {
        return new JpaSimulationReportRepository(autoTx);
    }

    @Override
    public WeatherDataRepository weatherData() {
        return new JpaWeatherDataRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public WeatherDataRepository weatherData(final TransactionalContext autoTx) {
        return new JpaWeatherDataRepository(autoTx);
    }

    @Override
    public AircraftModelRepository aircraftModels() {
        return new JpaAircraftModelRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public AircraftModelRepository aircraftModels(final TransactionalContext autoTx) {
        return new JpaAircraftModelRepository(autoTx);
    }

    @Override
    public AircraftRepository aircraft() {
        return new JpaAircraftRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public AircraftRepository aircraft(final TransactionalContext autoTx) {
        return new JpaAircraftRepository(autoTx);
    }

    @Override
    public AirControlAreaRepository airControlAreas() {
        return new JpaAirControlAreaRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public AirControlAreaRepository airControlAreas(final TransactionalContext autoTx) {
        return new JpaAirControlAreaRepository(autoTx);
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies() {
        return new JpaAirTransportCompanyRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies(final TransactionalContext autoTx) {
        return new JpaAirTransportCompanyRepository(autoTx);
    }

    @Override
    public AirportRepository airports() {
        return new JpaAirportRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public AirportRepository airports(final TransactionalContext autoTx) {
        return new JpaAirportRepository(autoTx);
    }

    @Override
    public PilotRepository pilots() {
        return new JpaPilotRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public PilotRepository pilots(final TransactionalContext autoTx) {
        return new JpaPilotRepository(autoTx);
    }

    @Override
    public SimulationRepository simulations() {
        return new JpaSimulationRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public SimulationRepository simulations(final TransactionalContext autoTx) {
        return new JpaSimulationRepository(autoTx);
    }

    @Override
    public CustomerCollaboratorRepository customerCollaborators() {
        return new JpaCustomerCollaboratorRepository(PersistenceSettings.getPersistenceUnitName());
    }
    @Override
    public CustomerCollaboratorRepository customerCollaborators(final TransactionalContext autoTx) {
        return new JpaCustomerCollaboratorRepository(autoTx);
    }
}
