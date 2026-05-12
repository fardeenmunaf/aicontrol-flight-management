package flight.dsl.ast;

import java.util.Objects;

public class FuelAst {

    private final Double quantity;
    private final String unit;

    public FuelAst(final Double quantity, final String unit) {
        this.quantity = Objects.requireNonNull(quantity, "quantity");
        this.unit = Objects.requireNonNull(unit, "unit");
    }

    public Double quantity() {
        return quantity;
    }

    public String unit() {
        return unit;
    }
}

