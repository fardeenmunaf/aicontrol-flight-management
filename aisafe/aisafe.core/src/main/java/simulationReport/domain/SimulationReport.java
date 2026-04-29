package simulationReport.domain;

import java.time.Instant;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;
import shared.domain.SimulationId;

@Entity
public class SimulationReport implements DomainEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer totalFlights;
    private String validationResult;
    private Instant generatedAt;

    @Embedded
    private SimulationId simulationId;

    public SimulationReport(final Integer totalFlights, final String validationResult,
            final Instant generatedAt, final SimulationId simulationId) {
        Preconditions.noneNull(totalFlights, validationResult, generatedAt, simulationId);
        if (totalFlights < 0) {
            throw new IllegalArgumentException("Total flights must be non-negative");
        }
        this.totalFlights = totalFlights;
        this.validationResult = validationResult;
        this.generatedAt = generatedAt;
        this.simulationId = simulationId;
    }

    protected SimulationReport() {
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

    public Integer totalFlights() {
        return totalFlights;
    }

    public String validationResult() {
        return validationResult;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public SimulationId simulationId() {
        return simulationId;
    }
}

