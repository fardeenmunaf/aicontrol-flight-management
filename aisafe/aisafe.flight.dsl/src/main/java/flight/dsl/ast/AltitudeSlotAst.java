package flight.dsl.ast;

public class AltitudeSlotAst {

    private final double min;
    private final String minUnit;
    private final double max;
    private final String maxUnit;
    private final double width;
    private final String widthUnit;

    public AltitudeSlotAst(final double min, final String minUnit,
                           final double max, final String maxUnit,
                           final double width, final String widthUnit) {
        this.min = min;
        this.minUnit = minUnit;
        this.max = max;
        this.maxUnit = maxUnit;
        this.width = width;
        this.widthUnit = widthUnit;
    }

    public double min() { return min; }
    public String minUnit() { return minUnit; }
    public double max() { return max; }
    public String maxUnit() { return maxUnit; }
    public double width() { return width; }
    public String widthUnit() { return widthUnit; }
}
