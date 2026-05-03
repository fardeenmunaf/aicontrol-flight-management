package enginemodel.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

@Embeddable
public class FuelProperties implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    private FuelType type;

    private Double density;
    private Double specificEnergy;

    protected FuelProperties() {
    }

    public FuelProperties(final FuelType type, final Double density, final Double specificEnergy) {
        Preconditions.noneNull(type, density, specificEnergy);

        if (density <= 0) {
            throw new IllegalArgumentException("Fuel density must be greater than zero");
        }
        if (specificEnergy <= 0) {
            throw new IllegalArgumentException("Fuel specific energy must be greater than zero");
        }

        this.type = type;
        this.density = density;
        this.specificEnergy = specificEnergy;
    }

    public FuelType type() {
        return type;
    }

    public Double density() {
        return density;
    }

    public Double specificEnergy() {
        return specificEnergy;
    }
}