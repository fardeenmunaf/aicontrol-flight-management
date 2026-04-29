package weatherData.domain;

import java.time.LocalDate;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;
import shared.domain.WeatherDataId;

@Entity
public class WeatherData implements DomainEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDate date;
    private Double windSpeed;
    private Double windDirection;

    @Embedded
    private WeatherDataId weatherDataId;

    public WeatherData(final LocalDate date, final Double windSpeed, final Double windDirection,
            final WeatherDataId weatherDataId) {
        Preconditions.noneNull(date, windSpeed, windDirection, weatherDataId);
        if (windSpeed < 0) {
            throw new IllegalArgumentException("Wind speed must be non-negative");
        }
        if (windDirection < 0 || windDirection > 360) {
            throw new IllegalArgumentException("Wind direction must be between 0 and 360 degrees");
        }
        this.date = date;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.weatherDataId = weatherDataId;
    }

    protected WeatherData() {
        // for ORM
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public Long identity() {
        return id;
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

    public WeatherDataId weatherDataId() {
        return weatherDataId;
    }
}
