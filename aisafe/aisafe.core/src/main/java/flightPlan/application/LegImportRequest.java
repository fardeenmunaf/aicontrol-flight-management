package flightPlan.application;

import java.util.List;
import java.util.Objects;

public class LegImportRequest {

    public final int sequenceNumber;
    public final String departureDate;
    public final String departureTime;
    public final String arrivalDate;
    public final String arrivalTime;
    public final double fuelQuantity;
    public final String departureAirportCode;
    public final String arrivalAirportCode;
    public final List<SegmentImportRequest> segments;

    public LegImportRequest(final int sequenceNumber,
                            final String departureDate, final String departureTime,
                            final String arrivalDate, final String arrivalTime,
                            final double fuelQuantity,
                            final String departureAirportCode,
                            final String arrivalAirportCode,
                            final List<SegmentImportRequest> segments) {
        this.sequenceNumber = sequenceNumber;
        this.departureDate = Objects.requireNonNull(departureDate, "departureDate");
        this.departureTime = Objects.requireNonNull(departureTime, "departureTime");
        this.arrivalDate = Objects.requireNonNull(arrivalDate, "arrivalDate");
        this.arrivalTime = Objects.requireNonNull(arrivalTime, "arrivalTime");
        this.fuelQuantity = fuelQuantity;
        this.departureAirportCode = Objects.requireNonNull(departureAirportCode, "departureAirportCode");
        this.arrivalAirportCode = Objects.requireNonNull(arrivalAirportCode, "arrivalAirportCode");
        this.segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
    }
}
