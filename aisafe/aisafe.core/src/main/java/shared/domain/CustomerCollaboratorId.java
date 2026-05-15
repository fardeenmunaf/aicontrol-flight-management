package shared.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class CustomerCollaboratorId implements ValueObject, Comparable<CustomerCollaboratorId> {

    private static final long serialVersionUID = 1L;

    @Column(name = "customer_collaborator_id")

    private String value;

    protected CustomerCollaboratorId() {
    }

    private CustomerCollaboratorId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("CustomerCollaboratorId cannot be null or empty");
        }
        this.value = value;
    }

    public static CustomerCollaboratorId valueOf(final String value) {
        return new CustomerCollaboratorId(value);
    }

    public static CustomerCollaboratorId newId() {
        return new CustomerCollaboratorId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final CustomerCollaboratorId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerCollaboratorId)) return false;
        final CustomerCollaboratorId that = (CustomerCollaboratorId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}