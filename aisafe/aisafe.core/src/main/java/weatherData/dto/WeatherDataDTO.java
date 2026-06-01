package weatherData.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class WeatherDataDTO {

    private final String acaCode;
    private final Double latitude1;
    private final Double longitude1;
    private final Double latitude2;
    private final Double longitude2;
    private final Double altitudeInf;
    private final Double altitudeSup;
    private final Double direction;
    private final Double windSpeed;
    private final LocalDate date;
    private final LocalTime startHour;
    private final LocalTime endHour;

    public WeatherDataDTO(
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

    public String acaCode() { return acaCode; }
    public Double latitude1() { return latitude1; }
    public Double longitude1() { return longitude1; }
    public Double latitude2() { return latitude2; }
    public Double longitude2() { return longitude2; }
    public Double altitudeInf() { return altitudeInf; }
    public Double altitudeSup() { return altitudeSup; }
    public Double direction() { return direction; }
    public Double windSpeed() { return windSpeed; }
    public LocalDate date() { return date; }
    public LocalTime startHour() { return startHour; }
    public LocalTime endHour() { return endHour; }
}