package airport.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import shared.domain.AirControlAreaId;
import shared.domain.AirportId;

@Entity
public class Airport implements AggregateRoot<AirportId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private AirportId airportId;

    private String name;
    private String town;
    private String country;
    private String iataCode;
    private String icaoCode;
    private Double elevation;

    @Embedded
    private AirportCoordinates coordinates;

    private AirControlAreaId airControlAreaId;

    public Airport(
            final AirportId airportId,
            final String name,
            final String town,
            final String country,
            final String iataCode,
            final String icaoCode,
            final Double elevation,
            final AirportCoordinates coordinates,
            final AirControlAreaId airControlAreaId) {

        Preconditions.noneNull(
                airportId, name, town, country, iataCode, icaoCode,
                elevation, coordinates, airControlAreaId);

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

        this.airportId = airportId;
        this.name = name;
        this.town = town;
        this.country = country;
        this.iataCode = iataCode;
        this.icaoCode = icaoCode;
        this.elevation = elevation;
        this.coordinates = coordinates;
        this.airControlAreaId = airControlAreaId;
    }

    protected Airport() {
        // for ORM
    }

    @Override
    public AirportId identity() {
        return airportId;
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

    public AirControlAreaId airControlAreaId() {
        return airControlAreaId;
    }
}