package weatherData.domain;

import java.time.LocalDateTime;

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


    private String airControlArea;
    private LocalDateTime dateTime;
    private Double windSpeed;
    private Double windDirection;
    private Double temperature;
    private Double pressure;

    public WeatherData(
            final String airControlArea,
            final LocalDateTime dateTime,
            final Double windDirection,
            final Double windSpeed,
            final Double temperature,
            final Double pressure) {

        Preconditions.noneNull(
                airControlArea, dateTime, windDirection,
                windSpeed, temperature, pressure
        );

        if (airControlArea.isBlank()) {
            throw new IllegalArgumentException("Air control area cannot be empty");
        }

        if (windSpeed < 0) {
            throw new IllegalArgumentException("Wind speed must be non-negative");
        }

        if (windDirection < 0 || windDirection > 360) {
            throw new IllegalArgumentException("Wind direction must be between 0 and 360 degrees");
        }


        if (pressure <= 0) {
            throw new IllegalArgumentException("Pressure must be positive");
        }

        if (temperature < -100 || temperature > 100) {
            throw new IllegalArgumentException("Temperature out of realistic range");
        }

        this.weatherDataId = WeatherDataId.newId();
        this.airControlArea = airControlArea;
        this.dateTime = dateTime;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.temperature = temperature;
        this.pressure = pressure;
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


    public String airControlArea() {
        return airControlArea;
    }

    public LocalDateTime dateTime() {
        return dateTime;
    }

    public Double windSpeed() {
        return windSpeed;
    }

    public Double windDirection() {
        return windDirection;
    }

    public Double temperature() {
        return temperature;
    }

    public Double pressure() {
        return pressure;
    }
}