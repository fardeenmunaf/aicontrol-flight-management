package aircraft.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class CabinConfiguration implements ValueObject {

    private int economyClassSeats;
    private int businessClassSeats;
    private int firstClassSeats;

    protected CabinConfiguration() {
        // for ORM
    }

    public CabinConfiguration(final int economyClassSeats,
                              final int firstClassSeats,
                              final int businessClassSeats) {

        if (economyClassSeats < 0) {
            throw new IllegalArgumentException("Economy class seats cannot be negative");
        }
        if (firstClassSeats < 0) {
            throw new IllegalArgumentException("First class seats cannot be negative");
        }
        if (businessClassSeats < 0) {
            throw new IllegalArgumentException("Business class seats cannot be negative");
        }

        this.economyClassSeats = economyClassSeats;
        this.firstClassSeats = firstClassSeats;
        this.businessClassSeats = businessClassSeats;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CabinConfiguration)) return false;
        final CabinConfiguration other = (CabinConfiguration) o;
        return economyClassSeats == other.economyClassSeats
                && businessClassSeats == other.businessClassSeats
                && firstClassSeats == other.firstClassSeats;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(economyClassSeats, businessClassSeats, firstClassSeats);
    }

    public int economyClassSeats() {
        return economyClassSeats;
    }

    public int firstClassSeats() {
        return firstClassSeats;
    }

    public int businessClassSeats() {
        return businessClassSeats;
    }
}
