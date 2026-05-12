package flight.dsl.ast;

import java.util.Objects;

public class CoordinateAst {

    private final Double latitude;
    private final Double longitude;
    private final Double altitude;
    private final String altitudeUnit;

    public CoordinateAst(final Double latitude, final Double longitude, final Double altitude, final String altitudeUnit) {
        this.latitude = Objects.requireNonNull(latitude, "latitude");
        this.longitude = Objects.requireNonNull(longitude, "longitude");
        this.altitude = Objects.requireNonNull(altitude, "altitude");
        this.altitudeUnit = Objects.requireNonNull(altitudeUnit, "altitudeUnit");
    }

    public Double latitude() {
        return latitude;
    }

    public Double longitude() {
        return longitude;
    }

    public Double altitude() {
        return altitude;
    }

    public String altitudeUnit() {
        return altitudeUnit;
    }
}

