package enginemodel.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class EngineModel implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(unique = true, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private EngineType type;

    private Double power;
    private Double efficiency;
    private Double thrustSeaLevelStopped;
    private Double thrustSeaLevelCruise;
    private Double thrustSpecificFuelConsumption;

    @Embedded
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
                type, power, efficiency, thrustSeaLevelStopped,
                thrustSeaLevelCruise, thrustSpecificFuelConsumption, maker, fuelProperties);

        if (StringPredicates.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Engine model name should neither be null nor empty");
        }

        if (power <= 0 || efficiency <= 0 || thrustSeaLevelStopped <= 0
                || thrustSeaLevelCruise <= 0 || thrustSpecificFuelConsumption <= 0) {
            throw new IllegalArgumentException("Engine model numeric attributes must be greater than zero");
        }

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
    public Long identity() {
        return id;
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
}