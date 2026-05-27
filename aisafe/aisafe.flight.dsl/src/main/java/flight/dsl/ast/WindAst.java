package flight.dsl.ast;

public class WindAst {

    private final double direction;
    private final double speed;

    public WindAst(final double direction, final double speed) {
        this.direction = direction;
        this.speed = speed;
    }

    public double direction() { return direction; }
    public double speed() { return speed; }
}
