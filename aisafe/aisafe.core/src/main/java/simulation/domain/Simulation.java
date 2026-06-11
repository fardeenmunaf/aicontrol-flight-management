package simulation.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import flight.domain.Flight;
import shared.domain.SimulationId;
import weatherData.domain.WeatherData;
import aircontrolarea.domain.AirControlArea;

@Entity
public class Simulation implements AggregateRoot<SimulationId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private SimulationId simulationId;

    private Instant startDateTime;
    private Instant endDateTime;

    @Enumerated(EnumType.STRING)
    private SimulationStatus status;

    @ManyToOne
    @JoinColumn(name = "airControlArea_id")
    private AirControlArea airControlArea;

    @ManyToMany
    private Set<Flight> trackedFlights = new HashSet<>();

    @ManyToMany
    private Set<WeatherData> capturedWeatherData = new HashSet<>();

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AircraftPosition> aircraftPositions = new HashSet<>();

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SafetyViolation> safetyViolations = new HashSet<>();

    public Simulation(final Instant startDateTime, final Instant endDateTime, final SimulationStatus status) {
        Preconditions.noneNull(status, startDateTime, endDateTime);
        if (startDateTime != null && endDateTime != null && endDateTime.isBefore(startDateTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.status = status;
        this.simulationId = SimulationId.newId();
    }

    protected Simulation() {
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

    public Set<Flight> trackedFlights() {
        return new HashSet<>(trackedFlights);
    }

    public Set<WeatherData> capturedWeatherData() {
        return new HashSet<>(capturedWeatherData);
    }
}
