package flightPlan.dto.export;

import java.util.List;

public record SimulationFlightPlanDTO(
    String flightDesignator,
    String type,
    RouteReferenceDTO route,
    AircraftSpecsDTO aircraft,
    EngineSpecsDTO engine,
    List<SimulationLegDTO> legs
) {

    public record RouteReferenceDTO(
        String reference,
        String origin,
        String destination
    ) {}

    public record AircraftSpecsDTO(
        double emptyWeight,
        double wingArea,
        double aspectRatio,
        double oswaldEfficiency,
        double cd0,
        int numberOfEngines
    ) {}

    public record EngineSpecsDTO(
        double thrustSeaLevelStopped,
        double thrustSeaLevelCruise,
        double thrustLapseRate,
        double thrustSpecificFuelConsumption
    ) {}

    public record SimulationLegDTO(
        AirportTimeDTO departure,
        AirportTimeDTO arrival,
        QuantityUnitDTO fuel,
        QuantityUnitDTO payload,
        List<SimulationSegmentDTO> segments
    ) {}

    public record AirportTimeDTO(
        String airport,
        String date,
        String time
    ) {}

    public record QuantityUnitDTO(
        double quantity,
        String unit
    ) {}

    public record SimulationSegmentDTO(
        String mode,
        CoordinateDTO from,
        CoordinateDTO to
    ) {}

    public record CoordinateDTO(
        double latitude,
        double longitude,
        double altitude,
        String altitudeUnit
    ) {}
}
