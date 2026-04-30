package weatherData.domain;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;

@Entity
public class WeatherData implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Long version;

    private LocalDate date;
    private Double windSpeed;
    private Double windDirection;

    public WeatherData(final LocalDate date, final Double windSpeed, final Double windDirection) {
        Preconditions.noneNull(date, windSpeed, windDirection);
        if (windSpeed < 0) {
            throw new IllegalArgumentException("Wind speed must be non-negative");
        }
        if (windDirection < 0 || windDirection > 360) {
            throw new IllegalArgumentException("Wind direction must be between 0 and 360 degrees");
        }
        this.date = date;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
    }

    protected WeatherData() {
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

    public LocalDate date() {
        return date;
    }

    public Double windSpeed() {
        return windSpeed;
    }

    public Double windDirection() {
        return windDirection;
    }
}
