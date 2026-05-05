package simulationReport.domain;

import java.time.Instant;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import shared.domain.SimulationId;
import shared.domain.SimulationReportId;

@Entity
public class SimulationReport implements AggregateRoot<SimulationReportId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverride(name = "value", column = @jakarta.persistence.Column(name = "simulation_report_id"))
    private SimulationReportId simulationReportId;

    private Integer totalFlights;
    private String validationResult;
    private Instant generatedAt;

    @jakarta.persistence.Embedded
    @AttributeOverride(name = "value", column = @jakarta.persistence.Column(name = "simulation_id"))
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
        this.simulationReportId = SimulationReportId.newId();
    }

    protected SimulationReport() {
        // for ORM
    }

    @Override
    public SimulationReportId identity() {
        return simulationReportId;
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

