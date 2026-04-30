package aircontrolarea.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class AirControlArea implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Long version;

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
    }

    protected AirControlArea() {
        // for ORM
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
}

