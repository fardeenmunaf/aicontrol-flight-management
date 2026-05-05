package weatherData.dto;

import java.time.LocalDateTime;

public class WeatherDataDTO {

    private final String airControlArea;
    private final LocalDateTime dateTime;
    private final Double windDirection;
    private final Double windSpeed;
    private final Double temperature;
    private final Double pressure;

    public WeatherDataDTO(
            final String airControlArea,
            final LocalDateTime dateTime,
            final Double windDirection,
            final Double windSpeed,
            final Double temperature,
            final Double pressure) {

        this.airControlArea = airControlArea;
        this.dateTime = dateTime;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.temperature = temperature;
        this.pressure = pressure;
    }

    public String airControlArea() { return airControlArea; }
    public LocalDateTime dateTime() { return dateTime; }
    public Double windDirection() { return windDirection; }
    public Double windSpeed() { return windSpeed; }
    public Double temperature() { return temperature; }
    public Double pressure() { return pressure; }
}