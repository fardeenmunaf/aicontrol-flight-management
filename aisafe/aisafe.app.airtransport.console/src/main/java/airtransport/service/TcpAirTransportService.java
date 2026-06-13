package airtransport.service;

import airtransport.tcp.AirTransportTcpClient;

import java.io.IOException;

public class TcpAirTransportService implements AirTransportService {

    private final AirTransportTcpClient client;

    public TcpAirTransportService(final AirTransportTcpClient client) {
        this.client = client;
    }

    @Override
    public String listAircraftModels() {
        return call(client::listAircraftModels);
    }

    @Override
    public String listFleet() {
        return call(client::listFleet);
    }

    @Override
    public String registerAircraft(final String reg, final String country, final String status,
                                   final int crew, final String modelId, final String engineId,
                                   final int econ, final int bus, final int first) {
        final String payload = reg + ";" + country + ";" + status + ";" + crew + ";"
                + modelId + ";" + engineId + ";" + econ + ";" + bus + ";" + first;
        return call(() -> client.registerAircraft(payload));
    }

    @Override
    public String decommissionAircraft(final String aircraftId) {
        return call(() -> client.decommissionAircraft(aircraftId));
    }

    @Override
    public String listAirports() {
        return call(client::listAirports);
    }

    @Override
    public String listFlightRoutes() {
        return call(client::listFlightRoutes);
    }

    @Override
    public String createFlightRoute(final String designation, final String originIata,
                                    final String destIata, final String activeFrom) {
        final String payload = designation + ";" + originIata + ";" + destIata + ";" + activeFrom;
        return call(() -> client.createFlightRoute(payload));
    }

    @Override
    public String deleteFlightRoute(final String routeId, final String deactivationDate) {
        return call(() -> client.deleteFlightRoute(routeId, deactivationDate));
    }

    @Override
    public String listFlightPlans() {
        return call(client::listFlightPlans);
    }

    @Override
    public String createFlightPlan(final String dslText) {
        return call(() -> client.createFlightPlan(dslText));
    }

    @Override
    public String validateFlightPlan(final String planId) {
        return call(() -> client.validateFlightPlan(planId));
    }

    @Override
    public String addWeatherData(final String planId) {
        return call(() -> client.addWeatherData(planId));
    }

    @Override
    public String listCompanyPilots() {
        return call(client::listCompanyPilots);
    }

    @Override
    public String listAvailablePilots() {
        return call(client::listAvailablePilots);
    }

    @Override
    public String addPilotToCompany(final String userId) {
        return call(() -> client.addPilot(userId));
    }

    @Override
    public String removePilotFromCompany(final String userId) {
        return call(() -> client.removePilot(userId));
    }

    @Override
    public String activatePilot(final String userId) {
        return call(() -> client.activatePilot(userId));
    }

    @Override
    public String deactivatePilot(final String userId) {
        return call(() -> client.deactivatePilot(userId));
    }

    private static String call(final IOSupplier op) {
        try {
            return op.get();
        } catch (final IOException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @FunctionalInterface
    private interface IOSupplier {
        String get() throws IOException;
    }
}
