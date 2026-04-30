package airtransportcompany.domain;

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
public class AirTransportCompany implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Long version;

    private String name;
    private String iataCode;
    private String icaoCode;

    public AirTransportCompany(final String name,
                               final String iataCode,
                               final String icaoCode) {

        Preconditions.noneNull(name, iataCode, icaoCode);
        if (StringPredicates.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Name should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(iataCode)) {
            throw new IllegalArgumentException("IATA code should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(icaoCode)) {
            throw new IllegalArgumentException("ICAO code should neither be null nor empty");
        }

        this.name = name;
        this.iataCode = iataCode;
        this.icaoCode = icaoCode;
    }

    protected AirTransportCompany() {
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

    public String name() {
        return name;
    }

    public String iataCode() {
        return iataCode;
    }

    public String icaoCode() {
        return icaoCode;
    }
}

