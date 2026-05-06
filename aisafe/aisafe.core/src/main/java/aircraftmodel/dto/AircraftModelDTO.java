package aircraftmodel.dto;

import aircraftmodel.domain.AircraftType;

import java.util.List;

public class AircraftModelDTO {

    public final String modelName;
    public final String makerName;
    public final String makerCountry;
    public final AircraftType type;
    public final Double emptyWeight;
    public final Double maximumTakeOffWeight;
    public final Double maximumZeroFuelWeight;
    public final Double maximumFuelCapacity;
    public final Double serviceCeiling;
    public final Double cruiseSpeed;
    public final Double wingArea;
    public final Double wingSpan;
    public final Double dragCoefficient;
    public final Double liftCoefficient;
    public final List<String> engineModelNames;

    public AircraftModelDTO(
            final String modelName,
            final String makerName,
            final String makerCountry,
            final AircraftType type,
            final Double emptyWeight,
            final Double maximumTakeOffWeight,
            final Double maximumZeroFuelWeight,
            final Double maximumFuelCapacity,
            final Double serviceCeiling,
            final Double cruiseSpeed,
            final Double wingArea,
            final Double wingSpan,
            final Double dragCoefficient,
            final Double liftCoefficient,
            final List<String> engineModelNames) {
        this.modelName = modelName;
        this.makerName = makerName;
        this.makerCountry = makerCountry;
        this.type = type;
        this.emptyWeight = emptyWeight;
        this.maximumTakeOffWeight = maximumTakeOffWeight;
        this.maximumZeroFuelWeight = maximumZeroFuelWeight;
        this.maximumFuelCapacity = maximumFuelCapacity;
        this.serviceCeiling = serviceCeiling;
        this.cruiseSpeed = cruiseSpeed;
        this.wingArea = wingArea;
        this.wingSpan = wingSpan;
        this.dragCoefficient = dragCoefficient;
        this.liftCoefficient = liftCoefficient;
        this.engineModelNames = engineModelNames;
    }
}

