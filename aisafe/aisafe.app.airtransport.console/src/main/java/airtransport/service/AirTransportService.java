package airtransport.service;

public interface AirTransportService {

    // Aircraft models
    String listAircraftModels();

    // Fleet
    String listFleet();
    String registerAircraft(String reg, String country, String status, int crew,
                            String modelId, String engineId, int econ, int bus, int first);
    String decommissionAircraft(String aircraftId);

    // Airports & routes
    String listAirports();
    String listFlightRoutes();
    String createFlightRoute(String designation, String originIata, String destIata, String activeFrom);
    String deleteFlightRoute(String routeId, String deactivationDate);

    // Flight plans
    String listFlightPlans();
    String createFlightPlan(String dslText);
    String validateFlightPlan(String planId);
    String addWeatherData(String planId);

    // Pilot management
    String listCompanyPilots();
    String listAvailablePilots();
    String addPilotToCompany(String userId);
    String removePilotFromCompany(String userId);
    String activatePilot(String userId);
    String deactivatePilot(String userId);
}
