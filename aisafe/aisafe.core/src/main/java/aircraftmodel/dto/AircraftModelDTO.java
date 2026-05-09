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
    public final int numberOfEngines;
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
            final int numberOfEngines,
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
        this.numberOfEngines = numberOfEngines;
        this.engineModelNames = engineModelNames;
    }

    @Override
    public String toString() {
        return "AircraftModelDTO{" +
                "modelName='" + modelName + '\'' +
                ", makerName='" + makerName + '\'' +
                ", makerCountry='" + makerCountry + '\'' +
                ", type=" + type +
                ", emptyWeight=" + emptyWeight +
                ", maximumTakeOffWeight=" + maximumTakeOffWeight +
                ", maximumZeroFuelWeight=" + maximumZeroFuelWeight +
                ", maximumFuelCapacity=" + maximumFuelCapacity +
                ", serviceCeiling=" + serviceCeiling +
                ", cruiseSpeed=" + cruiseSpeed +
                ", wingArea=" + wingArea +
                ", wingSpan=" + wingSpan +
                ", dragCoefficient=" + dragCoefficient +
                ", liftCoefficient=" + liftCoefficient +
                ", numberOfEngines=" + numberOfEngines +
                ", engineModelNames=" + engineModelNames +
                '}';
    }
}

