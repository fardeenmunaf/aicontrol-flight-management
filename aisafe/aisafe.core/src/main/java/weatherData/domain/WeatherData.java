package weatherData.domain;

import java.time.LocalDate;
import java.time.LocalTime;

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

    private String acaCode;
    private Double latitude1;
    private Double longitude1;
    private Double latitude2;
    private Double longitude2;
    private Double altitudeInf;
    private Double altitudeSup;
    private Double direction;
    private Double windSpeed;
    private LocalDate date;
    private LocalTime startHour;
    private LocalTime endHour;

    public WeatherData(
            final String acaCode,
            final Double latitude1,
            final Double longitude1,
            final Double latitude2,
            final Double longitude2,
            final Double altitudeInf,
            final Double altitudeSup,
            final Double direction,
            final Double windSpeed,
            final LocalDate date,
            final LocalTime startHour,
            final LocalTime endHour) {

        Preconditions.noneNull(
                acaCode, latitude1, longitude1, latitude2, longitude2,
                altitudeInf, altitudeSup, direction, windSpeed, date, startHour, endHour
        );

        if (acaCode.isBlank()) {
            throw new IllegalArgumentException("ACA code cannot be empty");
        }

        if (latitude1 < -90 || latitude1 > 90) {
            throw new IllegalArgumentException("Latitude 1 must be between -90 and 90");
        }

        if (latitude2 < -90 || latitude2 > 90) {
            throw new IllegalArgumentException("Latitude 2 must be between -90 and 90");
        }

        if (longitude1 < -180 || longitude1 > 180) {
            throw new IllegalArgumentException("Longitude 1 must be between -180 and 180");
        }

        if (longitude2 < -180 || longitude2 > 180) {
            throw new IllegalArgumentException("Longitude 2 must be between -180 and 180");
        }

        if (altitudeInf < 0) {
            throw new IllegalArgumentException("Altitude inferior must be non-negative");
        }

        if (altitudeSup < 0) {
            throw new IllegalArgumentException("Altitude superior must be non-negative");
        }

        if (altitudeInf > altitudeSup) {
            throw new IllegalArgumentException("Altitude inferior must be less than or equal to altitude superior");
        }

        if (direction < 0 || direction > 360) {
            throw new IllegalArgumentException("Direction must be between 0 and 360 degrees");
        }

        if (windSpeed < 0) {
            throw new IllegalArgumentException("Wind speed must be non-negative");
        }

        if (startHour.isAfter(endHour)) {
            throw new IllegalArgumentException("Start hour must be before or equal to end hour");
        }

        this.weatherDataId = WeatherDataId.newId();
        this.acaCode = acaCode;
        this.latitude1 = latitude1;
        this.longitude1 = longitude1;
        this.latitude2 = latitude2;
        this.longitude2 = longitude2;
        this.altitudeInf = altitudeInf;
        this.altitudeSup = altitudeSup;
        this.direction = direction;
        this.windSpeed = windSpeed;
        this.date = date;
        this.startHour = startHour;
        this.endHour = endHour;
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

    public String acaCode() {
        return acaCode;
    }

    public Double latitude1() {
        return latitude1;
    }

    public Double longitude1() {
        return longitude1;
    }

    public Double latitude2() {
        return latitude2;
    }

    public Double longitude2() {
        return longitude2;
    }

    public Double altitudeInf() {
        return altitudeInf;
    }

    public Double altitudeSup() {
        return altitudeSup;
    }

    public Double direction() {
        return direction;
    }

    public Double windSpeed() {
        return windSpeed;
    }

    public LocalDate date() {
        return date;
    }

    public LocalTime startHour() {
        return startHour;
    }

    public LocalTime endHour() {
        return endHour;
    }
}