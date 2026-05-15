package shared.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class WeatherDataId implements ValueObject, Comparable<WeatherDataId> {

    private static final long serialVersionUID = 1L;

    @Column(name = "weather_data_id")

    private String value;

    public WeatherDataId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Weather data id should neither be null nor empty");
        }
        this.value = value;
    }

    protected WeatherDataId() {
        // for ORM
    }

    public static WeatherDataId valueOf(final String value) {
        return new WeatherDataId(value);
    }

    public static WeatherDataId newId() {
        return new WeatherDataId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final WeatherDataId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WeatherDataId)) {
            return false;
        }

        final WeatherDataId other = (WeatherDataId) o;
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

