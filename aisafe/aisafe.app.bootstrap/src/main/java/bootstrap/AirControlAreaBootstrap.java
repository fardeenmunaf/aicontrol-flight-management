package bootstrap;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;

public class AirControlAreaBootstrap {

    private final AirControlAreaRepository repository;

    public AirControlAreaBootstrap(final AirControlAreaRepository repository) {
        this.repository = repository;
    }

    public void execute() {
        // lat,lon;lat,lon format — each entry defines a bounding polygon/area
        registerIfAbsent("LPPC",  200.0, "37.0,-9.5;37.0,-6.0;42.0,-6.0;42.0,-9.5");   // Lisbon FIR (Portugal Continental)
        registerIfAbsent("LPPO",  180.0, "36.5,-31.5;36.5,-24.0;40.0,-24.0;40.0,-31.5"); // Santa Maria Oceanic FIR (Azores)
        registerIfAbsent("GCCC",  150.0, "27.0,-18.5;27.0,-13.0;29.5,-13.0;29.5,-18.5"); // Canarias FIR
        registerIfAbsent("LECM",  220.0, "36.0,-9.0;36.0,4.5;44.0,4.5;44.0,-9.0");      // Madrid FIR
        registerIfAbsent("LFFF",  250.0, "42.5,-5.0;42.5,8.5;51.0,8.5;51.0,-5.0");      // Paris FIR
        registerIfAbsent("EDGG",  230.0, "47.5,6.0;47.5,15.0;52.5,15.0;52.5,6.0");      // Langen FIR (Germany)
        registerIfAbsent("EGTT",  210.0, "49.0,-8.0;49.0,2.0;61.0,2.0;61.0,-8.0");      // London FIR
        registerIfAbsent("EBBU",  190.0, "49.5,2.5;49.5,6.5;51.5,6.5;51.5,2.5");        // Brussels FIR
        registerIfAbsent("EHAA",  195.0, "51.0,2.5;51.0,7.5;55.5,7.5;55.5,2.5");        // Amsterdam FIR
        registerIfAbsent("LIBB",  240.0, "37.0,6.5;37.0,18.5;47.0,18.5;47.0,6.5");      // Roma FIR
    }

    private void registerIfAbsent(final String areaCode,
                                  final double minimumRequiredArrivalFuel,
                                  final String geographicBoundaries) {
        if (!repository.findByAreaCode(areaCode).isEmpty()) {
            return;
        }
        final var area = new AirControlArea(areaCode, minimumRequiredArrivalFuel, geographicBoundaries);
        repository.save(area);
    }
}