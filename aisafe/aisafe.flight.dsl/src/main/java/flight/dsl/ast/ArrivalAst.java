package flight.dsl.ast;

import java.util.Objects;

public class ArrivalAst {

    private final String airportCode;
    private final String date;
    private final String time;

    public ArrivalAst(final String airportCode, final String date, final String time) {
        this.airportCode = Objects.requireNonNull(airportCode, "airportCode");
        this.date = Objects.requireNonNull(date, "date");
        this.time = Objects.requireNonNull(time, "time");
    }

    public String airportCode() {
        return airportCode;
    }

    public String date() {
        return date;
    }

    public String time() {
        return time;
    }
}

