package aircraft.domain;

import aircraftmodel.domain.AircraftModel;
import enginemodel.domain.EngineModel;
import airtransportcompany.domain.AirTransportCompany;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import shared.domain.AircraftId;

@Entity
public class Aircraft implements AggregateRoot<AircraftId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private AircraftId aircraftId;

    @Embedded
    private CabinConfiguration cabinConfiguration;

    @ManyToOne
    private AirTransportCompany airTransportCompany;

    @ManyToOne
    private AircraftModel aircraftModel;

    @ManyToOne
    private EngineModel engineModel;

    private String registrationNumber; 
    private String registrationCountry;
    @Enumerated(EnumType.STRING)
    private AircraftOperationStatus operationStatus;
    private int flightCrewCount;

    public Aircraft(final String registrationNumber,
                    final String registrationCountry,
                    final AircraftOperationStatus operationStatus,
                    final int flightCrewCount,
                    final AircraftModel aircraftModel,
                    final EngineModel engineModel,
                    final CabinConfiguration cabinConfiguration,
                    final AirTransportCompany airTransportCompany) {

        Preconditions.noneNull(registrationNumber, registrationCountry, operationStatus, aircraftModel, engineModel, cabinConfiguration, airTransportCompany);

        if (!aircraftModel.supportedEngineModels().contains(engineModel)) {
            throw new IllegalArgumentException("The selected engine is not certified for this aircraft model");
        }
        if (StringPredicates.isNullOrEmpty(registrationNumber)) {
            throw new IllegalArgumentException("Registration number should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(registrationCountry)) {
            throw new IllegalArgumentException("Registration country should neither be null nor empty");
        }
        if (flightCrewCount < 0) {
            throw new IllegalArgumentException("Flight crew count cannot be negative");
        }

        this.registrationCountry = registrationCountry;
        this.registrationNumber = registrationNumber;
        this.operationStatus = operationStatus;
        this.flightCrewCount = flightCrewCount;
        this.aircraftModel = aircraftModel;
        this.engineModel = engineModel;
        this.cabinConfiguration = cabinConfiguration;
        this.airTransportCompany = airTransportCompany;
        this.aircraftId = AircraftId.newId();
    }

    protected Aircraft() {
        // for ORM
    }

    @Override
    public AircraftId identity() {
        return aircraftId;
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

    public String registrationNumber() {
        return aircraftId.toString();
    }

    public String registrationCountry() {
        return registrationCountry;
    }

    public int flightCrewCount() {
        return flightCrewCount;
    }

    public AircraftOperationStatus operationStatus() {
        return operationStatus;
    }

    public AircraftModel aircraftModel() {
        return aircraftModel;
    }

    public EngineModel engineModel() {
        return engineModel;
    }

    public CabinConfiguration cabinConfiguration() {
        return cabinConfiguration;
    }

    public AirTransportCompany airTransportCompany() {
        return airTransportCompany;
    }

    /**
     * Decommission this aircraft (retire it from service).
     * 
     * @throws IllegalStateException if the aircraft is already decommissioned
     */
    public void decommission() {
        if (this.operationStatus == AircraftOperationStatus.DECOMISSIONED) {
            throw new IllegalStateException("Aircraft is already decommissioned");
        }
        this.operationStatus = AircraftOperationStatus.DECOMISSIONED;
    }
}
