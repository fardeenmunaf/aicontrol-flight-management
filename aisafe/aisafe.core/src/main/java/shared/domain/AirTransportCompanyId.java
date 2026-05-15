package shared.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AirTransportCompanyId implements ValueObject, Comparable<AirTransportCompanyId> {

    private static final long serialVersionUID = 1L;

    @Column(name = "air_transport_company_id")

    private String value;

    public AirTransportCompanyId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Air transport company id should neither be null nor empty");
        }
        this.value = value;
    }

    protected AirTransportCompanyId() {
        // for ORM
    }

    public static AirTransportCompanyId valueOf(final String value) {
        return new AirTransportCompanyId(value);
    }

    public static AirTransportCompanyId newId() {
        return new AirTransportCompanyId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final AirTransportCompanyId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AirTransportCompanyId)) {
            return false;
        }

        final AirTransportCompanyId other = (AirTransportCompanyId) o;
        return this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return this.value;
    }
}