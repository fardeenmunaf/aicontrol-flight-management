package enginemodel.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import shared.domain.EngineModelId;

@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "maker_name"})
    }
)
public class EngineModel implements AggregateRoot<EngineModelId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "engine_model_id", nullable = false, unique = true))
    private EngineModelId engineModelId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private EngineType type;

    private Double power;
    private Double efficiency;
    private Double thrustSeaLevelStopped;
    private Double thrustSeaLevelCruise;
    private Double thrustSpecificFuelConsumption;

    @Embedded
        @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "maker_name", nullable = false)),
            @AttributeOverride(name = "country", column = @Column(name = "maker_country", nullable = false))
        })
    private EngineModelMaker maker;

    @Embedded
    private FuelProperties fuelProperties;

    protected EngineModel() {
        // for ORM
    }

    public EngineModel(
            final String name,
            final EngineType type,
            final Double power,
            final Double efficiency,
            final Double thrustSeaLevelStopped,
            final Double thrustSeaLevelCruise,
            final Double thrustSpecificFuelConsumption,
            final EngineModelMaker maker,
            final FuelProperties fuelProperties) {

        Preconditions.noneNull(
                name, type, power, efficiency, thrustSeaLevelStopped,
                thrustSeaLevelCruise, thrustSpecificFuelConsumption, maker, fuelProperties);

        if (StringPredicates.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Engine model name should neither be null nor empty");
        }

        if (power <= 0 || efficiency <= 0 || thrustSeaLevelStopped <= 0
                || thrustSeaLevelCruise <= 0 || thrustSpecificFuelConsumption <= 0) {
            throw new IllegalArgumentException("Engine model numeric attributes must be greater than zero");
        }

        this.engineModelId = EngineModelId.newId();
        this.name = name;
        this.type = type;
        this.power = power;
        this.efficiency = efficiency;
        this.thrustSeaLevelStopped = thrustSeaLevelStopped;
        this.thrustSeaLevelCruise = thrustSeaLevelCruise;
        this.thrustSpecificFuelConsumption = thrustSpecificFuelConsumption;
        this.maker = maker;
        this.fuelProperties = fuelProperties;
    }

    @Override
    public EngineModelId identity() {
        return engineModelId;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    public EngineModelId engineModelId() {
        return engineModelId;
    }

    public String name() {
        return name;
    }

    public EngineType type() {
        return type;
    }

    public Double power() {
        return power;
    }

    public Double efficiency() {
        return efficiency;
    }

    public Double thrustSeaLevelStopped() {
        return thrustSeaLevelStopped;
    }

    public Double thrustSeaLevelCruise() {
        return thrustSeaLevelCruise;
    }

    public Double thrustSpecificFuelConsumption() {
        return thrustSpecificFuelConsumption;
    }

    public EngineModelMaker maker() {
        return maker;
    }

    public FuelProperties fuelProperties() {
        return fuelProperties;
    }
}