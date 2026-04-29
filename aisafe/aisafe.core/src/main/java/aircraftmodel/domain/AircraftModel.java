package aircraftmodel.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import shared.domain.AircraftModelId;

@Entity
public class AircraftModel implements AggregateRoot<AircraftModelId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private AircraftModelId aircraftModelId;

    @Enumerated(EnumType.STRING)
    private AircraftType type;

    private Double emptyWeight;
    private Double maximumTakeOffWeight;
    private Double maximumZeroFuelWeight;
    private Double maximumFuelCapacity;
    private Double serviceCeiling;
    private Double cruiseSpeed;
    private Double wingArea;
    private Double wingSpan;
    private Double dragCoefficient;
    private Double liftCoefficient;

    @Embedded
    private AircraftModelMaker maker;

    public AircraftModel(
            final AircraftModelId aircraftModelId,
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
            final AircraftModelMaker maker) {

        Preconditions.noneNull(
                aircraftModelId, type, emptyWeight, maximumTakeOffWeight,
                maximumZeroFuelWeight, maximumFuelCapacity, serviceCeiling,
                cruiseSpeed, wingArea, wingSpan, dragCoefficient, liftCoefficient, maker);

        if (emptyWeight <= 0 || maximumTakeOffWeight <= 0 || maximumZeroFuelWeight <= 0
                || maximumFuelCapacity <= 0 || serviceCeiling <= 0 || cruiseSpeed <= 0
                || wingArea <= 0 || wingSpan <= 0 || dragCoefficient <= 0 || liftCoefficient <= 0) {
            throw new IllegalArgumentException("Aircraft model numeric attributes must be greater than zero");
        }

        if (maximumTakeOffWeight < emptyWeight) {
            throw new IllegalArgumentException("Maximum take-off weight must be greater than or equal to empty weight");
        }

        if (maximumZeroFuelWeight > maximumTakeOffWeight) {
            throw new IllegalArgumentException("Maximum zero fuel weight must be less than or equal to maximum take-off weight");
        }

        this.aircraftModelId = aircraftModelId;
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
        this.maker = maker;
    }

    protected AircraftModel() {
        // for ORM
    }

    @Override
    public AircraftModelId identity() {
        return aircraftModelId;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    public AircraftType type() {
        return type;
    }

    public Double emptyWeight() {
        return emptyWeight;
    }

    public Double maximumTakeOffWeight() {
        return maximumTakeOffWeight;
    }

    public Double maximumZeroFuelWeight() {
        return maximumZeroFuelWeight;
    }

    public Double maximumFuelCapacity() {
        return maximumFuelCapacity;
    }

    public Double serviceCeiling() {
        return serviceCeiling;
    }

    public Double cruiseSpeed() {
        return cruiseSpeed;
    }

    public Double wingArea() {
        return wingArea;
    }

    public Double wingSpan() {
        return wingSpan;
    }

    public Double dragCoefficient() {
        return dragCoefficient;
    }

    public Double liftCoefficient() {
        return liftCoefficient;
    }

    public AircraftModelMaker maker() {
        return maker;
    }
}