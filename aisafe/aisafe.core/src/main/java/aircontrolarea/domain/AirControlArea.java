package aircontrolarea.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;
import shared.domain.AirControlAreaId;

@Entity
public class AirControlArea implements AggregateRoot<AirControlAreaId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private AirControlAreaId airControlAreaId;

    private String areaCode;
    private double minimumRequiredArrivalFuel;
    private String geographicBoundaries;

    public AirControlArea(final String areaCode,
                          final double minimumRequiredArrivalFuel,
                          final String geographicBoundaries) {

        Preconditions.noneNull(areaCode, geographicBoundaries);
        if (StringPredicates.isNullOrEmpty(areaCode)) {
            throw new IllegalArgumentException("Area code should neither be null nor empty");
        }
        if (minimumRequiredArrivalFuel < 0) {
            throw new IllegalArgumentException("Minimum required arrival fuel cannot be negative");
        }
        if (StringPredicates.isNullOrEmpty(geographicBoundaries)) {
            throw new IllegalArgumentException("Geographic boundaries should neither be null nor empty");
        }

        this.areaCode = areaCode;
        this.minimumRequiredArrivalFuel = minimumRequiredArrivalFuel;
        this.geographicBoundaries = geographicBoundaries;
        this.airControlAreaId = AirControlAreaId.newId();
    }

    protected AirControlArea() {
        // for ORM
    }

    @Override
    public AirControlAreaId identity() {
        return airControlAreaId;
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

    public String areaCode() {
        return areaCode;
    }

    public double minimumRequiredArrivalFuel() {
        return minimumRequiredArrivalFuel;
    }

    public String geographicBoundaries() {
        return geographicBoundaries;
    }

    @Override
    public String toString() {
        return String.format(
                "Code: %s | MinFuel: %.2f | Boundaries: %s",
                areaCode,
                minimumRequiredArrivalFuel,
                geographicBoundaries
        );
    }

}

