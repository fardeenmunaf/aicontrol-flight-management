package flight.dsl.ast;

import java.util.Objects;

public class CruiseEntryAst implements ProfileEntryAst {

    private final Double speed;
    private final String speedUnit;

    public CruiseEntryAst(final Double speed, final String speedUnit) {
        this.speed = Objects.requireNonNull(speed, "speed");
        this.speedUnit = Objects.requireNonNull(speedUnit, "speedUnit");
    }

    public Double speed() {
        return speed;
    }

    public String speedUnit() {
        return speedUnit;
    }
}
