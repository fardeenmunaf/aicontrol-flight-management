package flightRoute.domain;

import java.util.regex.Pattern;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

@Embeddable
public class RouteDesignation implements ValueObject {

    private static final long serialVersionUID = 1L;

    /**
     * Two uppercase letters (company initials) followed by 1 to 4 digits.
     * Examples: TP123, BA1, LH9999
     */
    private static final Pattern DESIGNATION_PATTERN = Pattern.compile("^[A-Z]{2}[0-9]{1,4}$");

    private String designation;

    public RouteDesignation(final String designation) {
        Preconditions.nonEmpty(designation, "Route designation must not be null or empty");
        if (!DESIGNATION_PATTERN.matcher(designation).matches()) {
            throw new IllegalArgumentException(
                    "Route designation must follow the format: 2 uppercase letters + 1-4 digits (e.g. TP123). Got: " + designation);
        }
        this.designation = designation;
    }

    protected RouteDesignation() {
        // for ORM
    }

    public String value() {
        return designation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteDesignation)) return false;
        return this.designation.equals(((RouteDesignation) o).designation);
    }

    @Override
    public int hashCode() {
        return designation.hashCode();
    }

    @Override
    public String toString() {
        return designation;
    }
}