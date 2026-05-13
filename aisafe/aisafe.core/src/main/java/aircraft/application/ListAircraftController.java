package aircraft.application;

import aircraft.domain.Aircraft;
import aircraft.repositories.AircraftRepository;

public class ListAircraftController {

    private final AircraftRepository aircraftRepository;

    public ListAircraftController(final AircraftRepository aircraftRepository) {
        this.aircraftRepository = aircraftRepository;
    }

    public Iterable<Aircraft> listAll() {
        return aircraftRepository.findAll();
    }
}
