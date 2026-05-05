package bootstrap;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airport.domain.Airport;
import airport.domain.AirportCoordinates;
import airport.repositories.AirportRepository;

public class AirportBootstrap {

    private final AirportRepository airportRepository;
    private final AirControlAreaRepository airControlAreaRepository;

    public AirportBootstrap(
            final AirportRepository airportRepository,
            final AirControlAreaRepository airControlAreaRepository) {
        this.airportRepository = airportRepository;
        this.airControlAreaRepository = airControlAreaRepository;
    }

    public void execute() {
        registerIfAbsent("Francisco Sá Carneiro Airport", "Porto", "Portugal", "OPO", "LPPR",
                69.0, 41.2421, -8.6781, "PORTO_AREA");

        registerIfAbsent("Humberto Delgado Airport", "Lisbon", "Portugal", "LIS", "LPPT",
                114.0, 38.7742, -9.1342, "LISBON_AREA");
    }

    private void registerIfAbsent(
            final String name,
            final String town,
            final String country,
            final String iataCode,
            final String icaoCode,
            final Double elevation,
            final Double latitude,
            final Double longitude,
            final String airControlAreaId) {

        if (airportRepository.findByIataCode(iataCode).isPresent()
                || airportRepository.findByIcaoCode(icaoCode).isPresent()) {
            return;
        }

        final AirControlArea area = airControlAreaRepository.ofIdentity(
                        shared.domain.AirControlAreaId.valueOf(airControlAreaId))
                .orElseThrow(() -> new IllegalArgumentException("Air control area does not exist: " + airControlAreaId));

        final Airport airport = new Airport(
                name, town, country, iataCode, icaoCode, elevation,
                new AirportCoordinates(latitude, longitude), area);

        airportRepository.save(airport);
    }
}