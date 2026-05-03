package airport.domain;

import aircontrolarea.domain.AirControlArea;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

@Entity
public class Airport implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String name;
    private String town;
    private String country;

    @Column(unique = true, nullable = false)
    private String iataCode;

    @Column(unique = true, nullable = false)
    private String icaoCode;

    private Double elevation;

    @Embedded
    private AirportCoordinates coordinates;

    @ManyToOne
    private AirControlArea airControlArea;

    protected Airport() {
        // for ORM
    }

    public Airport(
            final String name,
            final String town,
            final String country,
            final String iataCode,
            final String icaoCode,
            final Double elevation,
            final AirportCoordinates coordinates,
            final AirControlArea airControlArea) {

        Preconditions.noneNull(name, town, country, iataCode, icaoCode, elevation, coordinates, airControlArea);

        if (StringPredicates.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Airport name should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(town)) {
            throw new IllegalArgumentException("Airport town should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(country)) {
            throw new IllegalArgumentException("Airport country should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(iataCode)) {
            throw new IllegalArgumentException("Airport IATA code should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(icaoCode)) {
            throw new IllegalArgumentException("Airport ICAO code should neither be null nor empty");
        }
        if (elevation < 0) {
            throw new IllegalArgumentException("Airport elevation should not be negative");
        }

        this.name = name;
        this.town = town;
        this.country = country;
        this.iataCode = iataCode;
        this.icaoCode = icaoCode;
        this.elevation = elevation;
        this.coordinates = coordinates;
        this.airControlArea = airControlArea;
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

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String town() {
        return town;
    }

    public String country() {
        return country;
    }

    public String iataCode() {
        return iataCode;
    }

    public String icaoCode() {
        return icaoCode;
    }

    public Double elevation() {
        return elevation;
    }

    public AirportCoordinates coordinates() {
        return coordinates;
    }

    public AirControlArea airControlArea() {
        return airControlArea;
    }
}