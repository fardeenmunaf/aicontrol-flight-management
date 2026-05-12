package flight.dsl.ast;

import java.util.Objects;

public class ClimbEntryAst implements ProfileEntryAst {

    private final Double altitude;
    private final String altitudeUnit;
    private final Double speed;
    private final String speedUnit;

    public ClimbEntryAst(final Double altitude, final String altitudeUnit, final Double speed, final String speedUnit) {
        this.altitude = Objects.requireNonNull(altitude, "altitude");
        this.altitudeUnit = Objects.requireNonNull(altitudeUnit, "altitudeUnit");
        this.speed = Objects.requireNonNull(speed, "speed");
        this.speedUnit = Objects.requireNonNull(speedUnit, "speedUnit");
    }

    public Double altitude() {
        return altitude;
    }

    public String altitudeUnit() {
        return altitudeUnit;
    }

    public Double speed() {
        return speed;
    }

    public String speedUnit() {
        return speedUnit;
    }
}

