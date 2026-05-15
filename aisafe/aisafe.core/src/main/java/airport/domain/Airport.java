package airport.domain;

import aircontrolarea.domain.AirControlArea;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import shared.domain.AirportId;

@Entity
public class Airport implements AggregateRoot<AirportId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "airport_id", nullable = false, unique = true))
    private AirportId airportId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String town;

    @Column(nullable = false)
    private String country;

    @Column(unique = true, nullable = false)
    private String iataCode;

    @Column(unique = true, nullable = false)
    private String icaoCode;

    @Column(nullable = false)
    private Double elevation;

    @Embedded
    private AirportCoordinates coordinates;

    @ManyToOne(optional = false)
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


        final String normalizedIata = iataCode.trim().toUpperCase();
        final String normalizedIcao = icaoCode.trim().toUpperCase();

        if (!normalizedIata.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Airport IATA code must have exactly 3 letters");
        }
        if (!normalizedIcao.matches("[A-Z]{4}")) {
            throw new IllegalArgumentException("Airport ICAO code must have exactly 4 letters");
        }

        this.airportId = AirportId.newId();
        this.name = name.trim();
        this.town = town.trim();
        this.country = country.trim();
        this.iataCode = normalizedIata;
        this.icaoCode = normalizedIcao;
        this.elevation = elevation;
        this.coordinates = coordinates;
        this.airControlArea = airControlArea;

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
    public boolean equals(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    public AirportId airportId() {
        return airportId;
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