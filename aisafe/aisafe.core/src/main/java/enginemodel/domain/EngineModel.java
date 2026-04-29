package enginemodel.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import shared.domain.EngineModelId;

@Entity
public class EngineModel implements AggregateRoot<EngineModelId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private EngineModelId engineModelId;

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
            @AttributeOverride(name = "name", column = @jakarta.persistence.Column(name = "maker_name")),
            @AttributeOverride(name = "country", column = @jakarta.persistence.Column(name = "maker_country"))
    })
    private EngineModelMaker maker;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "type", column = @jakarta.persistence.Column(name = "fuel_type")),
            @AttributeOverride(name = "density", column = @jakarta.persistence.Column(name = "fuel_density")),
            @AttributeOverride(name = "specificEnergy", column = @jakarta.persistence.Column(name = "fuel_specific_energy"))
    })
    private FuelProperties fuelProperties;

    public EngineModel(
            final EngineModelId engineModelId,
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
                engineModelId, name, type, power, efficiency,
                thrustSeaLevelStopped, thrustSeaLevelCruise,
                thrustSpecificFuelConsumption, maker, fuelProperties);

        if (StringPredicates.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Engine model name should neither be null nor empty");
        }

        if (power <= 0 || efficiency <= 0 || thrustSeaLevelStopped <= 0
                || thrustSeaLevelCruise <= 0 || thrustSpecificFuelConsumption <= 0) {
            throw new IllegalArgumentException("Engine model numeric attributes must be greater than zero");
        }

        this.engineModelId = engineModelId;
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

    protected EngineModel() {
        // for ORM
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
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
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