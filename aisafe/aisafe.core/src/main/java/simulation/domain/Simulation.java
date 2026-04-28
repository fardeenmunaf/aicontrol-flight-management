package simulation.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import shared.domain.AirControlAreaId;
import shared.domain.FlightId;
import shared.domain.WeatherDataId;

@Entity
public class Simulation implements AggregateRoot<SimulationId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private SimulationId simulationId;

    private Instant startDateTime;
    private Instant endDateTime;

    private String geographicArea;
    private String safetyThresholds;
    private String performanceSettings;

    @Enumerated(EnumType.STRING)
    private SimulationStatus status;

    @Embedded
    // TODO: AirControlAreaId will be implemented by another team; compilation will fail until then.
    private AirControlAreaId airControlAreaId;

    @ElementCollection
    // TODO: FlightId will be implemented by another team; compilation will fail until then.
    private Set<FlightId> trackedFlights = new HashSet<>();

    @ElementCollection
    // TODO: WeatherDataId will be implemented by another team; compilation will fail until then.
    private Set<WeatherDataId> capturedWeatherDataIds = new HashSet<>();

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AircraftPosition> aircraftPositions = new HashSet<>();

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SafetyViolation> safetyViolations = new HashSet<>();

    public Simulation(final SimulationId simulationId, final Instant startDateTime, final Instant endDateTime, final SimulationStatus status, final AirControlAreaId airControlAreaId) {
        Preconditions.noneNull(simulationId, status, airControlAreaId);
        if (startDateTime != null && endDateTime != null && endDateTime.isBefore(startDateTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.simulationId = simulationId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.status = status;
        this.airControlAreaId = airControlAreaId;
    }

    protected Simulation() {
        // for ORM
    }

    public SimulationId identity() {
        return simulationId;
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

    public SimulationStatus status() {
        return status;
    }

    public Instant startDateTime() {
        return startDateTime;
    }

    public Instant endDateTime() {
        return endDateTime;
    }

    public AirControlAreaId airControlAreaId() {
        return airControlAreaId;
    }

    public Set<FlightId> trackedFlights() {
        return new HashSet<>(trackedFlights);
    }

    public Set<WeatherDataId> capturedWeatherDataIds() {
        return new HashSet<>(capturedWeatherDataIds);
    }
}
