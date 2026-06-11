package flightPlan.dto.export;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SimulationFlightPlanDTO(
    @JsonProperty("flightDesignator") String flightDesignator,
    @JsonProperty("type") String type,
    @JsonProperty("route") RouteReferenceDTO route,
    @JsonProperty("aircraft") AircraftSpecsDTO aircraft,
    @JsonProperty("engine") EngineSpecsDTO engine,
    @JsonProperty("legs") List<SimulationLegDTO> legs
) {

    public record RouteReferenceDTO(
        @JsonProperty("reference") String reference,
        @JsonProperty("origin") String origin,
        @JsonProperty("destination") String destination
    ) {}

    public record AircraftSpecsDTO(
        @JsonProperty("emptyWeight") double emptyWeight,
        @JsonProperty("wingArea") double wingArea,
        @JsonProperty("aspectRatio") double aspectRatio,
        @JsonProperty("oswaldEfficiency") double oswaldEfficiency,
        @JsonProperty("cd0") double cd0,
        @JsonProperty("numberOfEngines") int numberOfEngines
    ) {}

    public record EngineSpecsDTO(
        @JsonProperty("thrustSeaLevelStopped") double thrustSeaLevelStopped,
        @JsonProperty("thrustSeaLevelCruise") double thrustSeaLevelCruise,
        @JsonProperty("thrustLapseRate") double thrustLapseRate,
        @JsonProperty("thrustSpecificFuelConsumption") double thrustSpecificFuelConsumption
    ) {}

    public record LegEndpointDTO(
        @JsonProperty("airport") String airport,
        @JsonProperty("date") String date,
        @JsonProperty("time") String time
    ) {}

    public record SimulationLegDTO(
        @JsonProperty("departure") LegEndpointDTO departure,
        @JsonProperty("arrival") LegEndpointDTO arrival,
        @JsonProperty("fuel") QuantityDTO fuel,
        @JsonProperty("payload") QuantityDTO payload,
        @JsonProperty("flightProfile") ProfileDTO flightProfile,
        @JsonProperty("segments") List<SimulationSegmentDTO> segments
    ) {}

    /** Used for fuel and payload: {"quantity": ..., "unit": ...} */
    public record QuantityDTO(
        @JsonProperty("quantity") double quantity,
        @JsonProperty("unit") String unit
    ) {}

    /** Used for speed/altitude measurements: {"value": ..., "unit": ...} */
    public record MeasureDTO(
        @JsonProperty("value") double value,
        @JsonProperty("unit") String unit
    ) {}

    public record ProfileDTO(
        @JsonProperty("climb") List<ClimbEntryDTO> climb,
        @JsonProperty("cruise") CruiseEntryDTO cruise,
        @JsonProperty("descend") List<DescendEntryDTO> descend
    ) {}

    public record ClimbEntryDTO(
        @JsonProperty("altitude") MeasureDTO altitude,
        @JsonProperty("speed") MeasureDTO speed
    ) {}

    public record CruiseEntryDTO(
        @JsonProperty("speed") MeasureDTO speed
    ) {}

    public record DescendEntryDTO(
        @JsonProperty("altitude") MeasureDTO altitude,
        @JsonProperty("speed") MeasureDTO speed,
        @JsonProperty("rateDescent") MeasureDTO rateDescent
    ) {}

    public record SimulationSegmentDTO(
        @JsonProperty("mode") String mode,
        @JsonProperty("from") CoordinateDTO from,
        @JsonProperty("to") CoordinateDTO to,
        @JsonProperty("altitudeSlot") AltitudeSlotDTO altitudeSlot,
        @JsonProperty("wind") WindDTO wind
    ) {}

    public record CoordinateDTO(
        @JsonProperty("latitude") double latitude,
        @JsonProperty("longitude") double longitude,
        @JsonProperty("altitude") double altitude,
        @JsonProperty("altitudeUnit") String altitudeUnit
    ) {}

    public record AltitudeSlotDTO(
        @JsonProperty("min") MeasureDTO min,
        @JsonProperty("max") MeasureDTO max,
        @JsonProperty("width") MeasureDTO width
    ) {}

    public record WindDTO(
        @JsonProperty("direction") double direction,
        @JsonProperty("speed") MeasureDTO speed
    ) {}
}
