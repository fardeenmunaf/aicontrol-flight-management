package airport.application;

import airport.domain.Airport;
import airport.repositories.AirportRepository;

public class ListAirportsController {

    private final AirportRepository airportRepository;

    public ListAirportsController(final AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    public Iterable<Airport> listAirports() {
        return airportRepository.findAll();
    }
}
