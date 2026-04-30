package aircraft.domain;

import aircraftmodel.domain.AircraftModel;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

@Entity
public class Aircraft implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Long version;

    @Embedded
    private CabinConfiguration cabinConfiguration;

    @ManyToOne
    private AircraftModel aircraftModel;

    private String registrationNumber; //int ou string?
    private String registrationCountry;
    private String operationStatus;
    private int flightCrewCount;

    public Aircraft(final String registrationNumber,
                    final String registrationCountry,
                    final String operationStatus,
                    final int flightCrewCount,
                    final AircraftModel aircraftModel,
                    final CabinConfiguration cabinConfiguration) {

        Preconditions.noneNull(registrationNumber, registrationCountry, operationStatus, aircraftModel, cabinConfiguration);
        if (StringPredicates.isNullOrEmpty(registrationNumber)) {
            throw new IllegalArgumentException("Registration number should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(registrationCountry)) {
            throw new IllegalArgumentException("Registration country should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(operationStatus)) {
            throw new IllegalArgumentException("Operation status should neither be null nor empty");
        }
        if (flightCrewCount < 0) {
            throw new IllegalArgumentException("Flight crew count cannot be negative");
        }

        this.registrationNumber = registrationNumber;
        this.registrationCountry = registrationCountry;
        this.operationStatus = operationStatus;
        this.flightCrewCount = flightCrewCount;
        this.aircraftModel = aircraftModel;
        this.cabinConfiguration = cabinConfiguration;
    }

    protected Aircraft() {
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

    public String registrationNumber() {
        return registrationNumber;
    }

    public String registrationCountry() {
        return registrationCountry;
    }

    public int flightCrewCount() {
        return flightCrewCount;
    }

    public String operationStatus() {
        return operationStatus;
    }

    public AircraftModel aircraftModel() {
        return aircraftModel;
    }

    public CabinConfiguration cabinConfiguration() {
        return cabinConfiguration;
    }
}
