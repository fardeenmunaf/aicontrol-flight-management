
package bootstrap;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airport.domain.Airport;
import airport.domain.AirportCoordinates;
import airport.repositories.AirportRepository;

import java.util.List;

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
                69.0, 41.2421, -8.6781, "LPPC");

        registerIfAbsent("Humberto Delgado Airport", "Lisbon", "Portugal", "LIS", "LPPT",
                114.0, 38.7742, -9.1342, "LPPC");

        registerIfAbsent("Adolfo Suarez Madrid-Barajas Airport", "Madrid", "Spain", "MAD", "LEMD",
                610.0, 40.4895, -3.5643, "LECM");

        registerIfAbsent("Josep Tarradellas Barcelona-El Prat Airport", "Barcelona", "Spain", "BCN", "LEBL",
                4.0, 41.2971, 2.0785, "LECM");
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
            final String areaCode) {

        if (airportRepository.findByIataCode(iataCode).isPresent()
                || airportRepository.findByIcaoCode(icaoCode).isPresent()) {
            return;
        }

        final List<AirControlArea> areas = airControlAreaRepository.findByAreaCode(areaCode);
        if (areas.isEmpty()) {
            System.out.println("Air control area not found, skipping airport: " + iataCode + " (area=" + areaCode + ")");
            return;
        }

        final Airport airport = new Airport(
                name, town, country, iataCode, icaoCode, elevation,
                new AirportCoordinates(latitude, longitude), areas.get(0));

        airportRepository.save(airport);
        System.out.println("Airport bootstrapped: " + iataCode + " (" + icaoCode + ")");
    }
}
