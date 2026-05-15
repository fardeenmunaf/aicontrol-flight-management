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
        registerIfAbsent("LPPC", 200.0, "37.0,-9.5;37.0,-6.0;42.0,-6.0;42.0,-9.5"); // Lisbon FIR (Portugal Continental)
        registerIfAbsent("LPPO", 180.0, "36.5,-31.5;36.5,-24.0;40.0,-24.0;40.0,-31.5"); // Santa Maria Oceanic FIR
                                                                                        // (Azores)
        registerIfAbsent("LPMA", 150.0, "32.0,-18.0;32.0,-15.0;34.0,-15.0;34.0,-18.0"); // Madeira TMA
        registerIfAbsent("GCCC", 150.0, "27.0,-18.5;27.0,-13.0;29.5,-13.0;29.5,-18.5"); // Canarias FIR
        registerIfAbsent("LECM", 220.0, "36.0,-9.0;36.0,4.5;44.0,4.5;44.0,-9.0"); // Madrid FIR
        registerIfAbsent("LFFF", 250.0, "42.5,-5.0;42.5,8.5;51.0,8.5;51.0,-5.0"); // Paris FIR
        registerIfAbsent("EDGG", 230.0, "47.5,6.0;47.5,15.0;55.0,15.0;55.0,6.0"); // Langen FIR (Germany - expanded for
                                                                                  // BER)
        registerIfAbsent("EGTT", 210.0, "49.0,-8.0;49.0,2.0;61.0,2.0;61.0,-8.0"); // London FIR
        registerIfAbsent("EISN", 200.0, "51.0,-11.0;51.0,-5.0;56.0,-5.0;56.0,-11.0"); // Shannon FIR (Ireland)
        registerIfAbsent("BIRD", 250.0, "63.0,-25.0;63.0,-13.0;67.0,-13.0;67.0,-25.0"); // Reykjavik FIR (Iceland)
        registerIfAbsent("ENOR", 220.0, "58.0,4.0;58.0,32.0;71.0,32.0;71.0,4.0"); // Norway FIR
        registerIfAbsent("EBBU", 190.0, "49.5,2.5;49.5,6.5;51.5,6.5;51.5,2.5"); // Brussels FIR
        registerIfAbsent("EHAA", 195.0, "51.0,2.5;51.0,7.5;55.5,7.5;55.5,2.5"); // Amsterdam FIR
        registerIfAbsent("LIBB", 240.0, "37.0,6.5;37.0,18.5;47.0,18.5;47.0,6.5"); // Roma FIR
        registerIfAbsent("LSAS", 180.0, "45.5,5.5;45.5,11.0;48.0,11.0;48.0,5.5"); // Switzerland FIR (ZRH)
        registerIfAbsent("KUSA", 300.0, "24.0,-125.0;24.0,-65.0;50.0,-65.0;50.0,-125.0"); // USA FIR broad box
        registerIfAbsent("PHZH", 200.0, "18.0,-162.0;18.0,-154.0;23.0,-154.0;23.0,-162.0"); // Hawaii FIR
        registerIfAbsent("CZEG", 250.0, "50.0,-140.0;50.0,-50.0;70.0,-50.0;70.0,-140.0"); // Canada FIR broad box
        registerIfAbsent("MMFR", 200.0, "14.0,-120.0;14.0,-85.0;33.0,-85.0;33.0,-120.0"); // Mexico FIR
        registerIfAbsent("SKED", 200.0, "-5.0,-85.0;-5.0,-65.0;15.0,-65.0;15.0,-85.0"); // Colombia/North SA FIR
        registerIfAbsent("SBAZ", 300.0, "-35.0,-75.0;-35.0,-30.0;5.0,-30.0;5.0,-75.0"); // Brazil FIR broad box
        registerIfAbsent("UUWV", 300.0, "40.0,30.0;40.0,140.0;75.0,140.0;75.0,30.0"); // Russia/Kazakhstan FIR
        registerIfAbsent("FAJA", 250.0, "-35.0,10.0;-35.0,40.0;-20.0,40.0;-20.0,10.0"); // South Africa FIR
        registerIfAbsent("FZZA", 250.0, "-20.0,10.0;-20.0,40.0;15.0,40.0;15.0,10.0"); // Central Africa FIR
        registerIfAbsent("RJJJ", 250.0, "25.0,125.0;25.0,150.0;45.0,150.0;45.0,125.0"); // Japan FIR
        registerIfAbsent("ZBAA", 300.0, "15.0,75.0;15.0,135.0;55.0,135.0;55.0,75.0"); // China FIR broad box
        registerIfAbsent("WSJC", 250.0, "-10.0,90.0;-10.0,120.0;15.0,120.0;15.0,90.0"); // SE Asia FIR
        registerIfAbsent("YBBB", 300.0, "-45.0,110.0;-45.0,160.0;-10.0,160.0;-10.0,110.0"); // Australia FIR
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