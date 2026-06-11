package flightPlan.dto.export;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SimulationFlightPlanDTO(
    @JsonProperty("FlightDesignator") String flightDesignator,
    @JsonProperty("Type") String type,
    @JsonProperty("Route") RouteReferenceDTO route,
    @JsonProperty("Aircraft") AircraftSpecsDTO aircraft,
    @JsonProperty("Engine") EngineSpecsDTO engine,
    @JsonProperty("Legs") List<SimulationLegDTO> legs
) {

    public record RouteReferenceDTO(
        @JsonProperty("Reference") String reference,
        @JsonProperty("Origin") String origin,
        @JsonProperty("Destination") String destination
    ) {}

    public record AircraftSpecsDTO(
        @JsonProperty("EmptyWeight") double emptyWeight,
        @JsonProperty("WingArea") double wingArea,
        @JsonProperty("AspectRatio") double aspectRatio,
        @JsonProperty("OswaldEfficiency") double oswaldEfficiency,
        @JsonProperty("Cd0") double cd0,
        @JsonProperty("NumberOfEngines") int numberOfEngines
    ) {}

    public record EngineSpecsDTO(
        @JsonProperty("ThrustSeaLevelStopped") double thrustSeaLevelStopped,
        @JsonProperty("ThrustSeaLevelCruise") double thrustSeaLevelCruise,
        @JsonProperty("ThrustLapseRate") double thrustLapseRate,
        @JsonProperty("ThrustSpecificFuelConsumption") double thrustSpecificFuelConsumption
    ) {}

    public record SimulationLegDTO(
        @JsonProperty("Departure") String departure,
        @JsonProperty("DepartureDate") String departureDate,
        @JsonProperty("DepartureTime") String departureTime,
        @JsonProperty("Arrival") String arrival,
        @JsonProperty("ArrivalDate") String arrivalDate,
        @JsonProperty("ArrivalTime") String arrivalTime,
        @JsonProperty("Fuel") QuantityDTO fuel,
        @JsonProperty("Payload") QuantityDTO payload,
        @JsonProperty("FlightProfile") ProfileDTO flightProfile,
        @JsonProperty("Segments") List<SimulationSegmentDTO> segments
    ) {}

    /** Used for fuel and payload: {"Quantity": ..., "Unit": ...} */
    public record QuantityDTO(
        @JsonProperty("Quantity") double quantity,
        @JsonProperty("Unit") String unit
    ) {}

    /** Used for all other measurements: {"Value": ..., "Unit": ...} */
    public record MeasureDTO(
        @JsonProperty("Value") double value,
        @JsonProperty("Unit") String unit
    ) {}

    public record ProfileDTO(
        @JsonProperty("Climb") List<ClimbEntryDTO> climb,
        @JsonProperty("Cruise") CruiseEntryDTO cruise,
        @JsonProperty("Descend") List<DescendEntryDTO> descend
    ) {}

    public record ClimbEntryDTO(
        @JsonProperty("Altitude") MeasureDTO altitude,
        @JsonProperty("Speed") MeasureDTO speed
    ) {}

    public record CruiseEntryDTO(
        @JsonProperty("Speed") MeasureDTO speed
    ) {}

    public record DescendEntryDTO(
        @JsonProperty("Altitude") MeasureDTO altitude,
        @JsonProperty("Speed") MeasureDTO speed,
        @JsonProperty("RateDescent") MeasureDTO rateDescent
    ) {}

    public record SimulationSegmentDTO(
        @JsonProperty("Mode") String mode,
        @JsonProperty("Start") CoordinateDTO start,
        @JsonProperty("End") CoordinateDTO end,
        @JsonProperty("AltitudeSlot") AltitudeSlotDTO altitudeSlot,
        @JsonProperty("Wind") WindDTO wind
    ) {}

    public record CoordinateDTO(
        @JsonProperty("Latitude") double latitude,
        @JsonProperty("Longitude") double longitude,
        @JsonProperty("Altitude") MeasureDTO altitude
    ) {}

    public record AltitudeSlotDTO(
        @JsonProperty("Min") MeasureDTO min,
        @JsonProperty("Max") MeasureDTO max,
        @JsonProperty("Width") MeasureDTO width
    ) {}

    public record WindDTO(
        @JsonProperty("Direction") double direction,
        @JsonProperty("Speed") MeasureDTO speed
    ) {}
}
