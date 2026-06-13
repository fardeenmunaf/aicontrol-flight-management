package airtransport;

import airtransport.service.AirTransportService;

public final class AirTransportContext {

    private static AirTransportService service;

    public static void setService(final AirTransportService s) {
        service = s;
    }

    public static AirTransportService service() {
        if (service == null) throw new IllegalStateException("AirTransportService not initialised");
        return service;
    }

    private AirTransportContext() {}
}
