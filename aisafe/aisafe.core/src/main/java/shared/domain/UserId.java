package shared.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class UserId implements ValueObject, Comparable<UserId> {

    private static final long serialVersionUID = 1L;

    private String value;

    public UserId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("User id should neither be null nor empty");
        }
        this.value = value;
    }

    protected UserId() {
        // for ORM
    }

    public static UserId valueOf(final String value) {
        return new UserId(value);
    }

    @Override
    public int compareTo(final UserId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserId)) {
            return false;
        }

        final UserId other = (UserId) o;
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
