package weatherData.domain;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import shared.domain.WeatherDataId;

@Entity
public class WeatherData implements AggregateRoot<WeatherDataId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private WeatherDataId weatherDataId;

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
        this.weatherDataId = WeatherDataId.newId();
        this.date = date;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
    }

    protected WeatherData() {
        // for ORM
    }

    @Override
    public WeatherDataId identity() {
        return weatherDataId;
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
