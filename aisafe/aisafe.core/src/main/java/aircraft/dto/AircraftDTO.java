package aircraft.dto;

/**
 * Data Transfer Object for Aircraft.
 * Used to represent aircraft fleet information with minimum required fields.
 *
 * @author LAPR4 Team
 */
public class AircraftDTO {

    public final String registrationNumber;
    public final String model;
    public final String operationalStatus;
    public final String cabinConfig;
    public final String registrationCountry;

    public AircraftDTO(
            final String registrationNumber,
            final String model,
            final String operationalStatus,
            final String cabinConfig,
            final String registrationCountry) {
        this.registrationNumber = registrationNumber;
        this.model = model;
        this.operationalStatus = operationalStatus;
        this.cabinConfig = cabinConfig;
        this.registrationCountry = registrationCountry;
    }

    @Override
    public String toString() {
        return "AircraftDTO{" +
                "registrationNumber='" + registrationNumber + '\'' +
                ", model='" + model + '\'' +
                ", operationalStatus='" + operationalStatus + '\'' +
                ", cabinConfig='" + cabinConfig + '\'' +
                ", registrationCountry='" + registrationCountry + '\'' +
                '}';
    }
}

