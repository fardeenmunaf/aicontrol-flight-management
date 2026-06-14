package simulation.application;

import aircontrolarea.domain.AirControlArea;
import simulation.domain.SafetyViolation;
import simulation.domain.Simulation;
import simulation.domain.SimulationFlight;
import simulation.domain.SimulationStatus;
import simulation.repository.SimulationRepository;

import java.time.Instant;

public class RecordSimulationResultService {

    /** Resolves the air control area for a flight callsign (null if unknown). */
    @FunctionalInterface
    public interface AreaResolver { AirControlArea areaFor(String callsign); }

    /**
     * Resolves the real scheduled departure date for a flight callsign.
     * If the date is unknown, implementations should return {@code fallback}.
     */
    @FunctionalInterface
    public interface DepartureDateResolver {
        Instant departureDateFor(String callsign, Instant fallback);
    }

    private final SimulationRepository simulations;
    private final AreaResolver areaResolver;
    private final DepartureDateResolver departureDateResolver;

    /**
     * Full constructor with both resolvers.
     */
    public RecordSimulationResultService(final SimulationRepository simulations,
                                         final AreaResolver areaResolver,
                                         final DepartureDateResolver departureDateResolver) {
        this.simulations = simulations;
        this.areaResolver = areaResolver;
        this.departureDateResolver = departureDateResolver;
    }

    /**
     * Convenience constructor that falls back to the simulation start time for every flight's
     * departure date (preserves backward compatibility with callers that only supply an area resolver).
     */
    public RecordSimulationResultService(final SimulationRepository simulations,
                                         final AreaResolver areaResolver) {
        this(simulations, areaResolver, (cs, fallback) -> fallback);
    }

    public Simulation record(final SimulationResultDTO dto) {
        final Simulation sim = new Simulation(dto.startDateTime(), dto.endDateTime(), SimulationStatus.COMPLETED);
        for (final var f : dto.flights()) {
            final Instant depDate = departureDateResolver.departureDateFor(f.callsign(), dto.startDateTime());
            sim.addFlight(new SimulationFlight(f.callsign(), depDate,
                    f.completed(), f.hadViolation(), areaResolver.areaFor(f.callsign())));
        }
        for (final var v : dto.violations()) {
            final AirControlArea area = v.callsigns().isEmpty() ? null : areaResolver.areaFor(v.callsigns().get(0));
            sim.addViolation(new SafetyViolation(sim, v.timestamp(), v.violationType(), area,
                    new java.util.HashSet<>(v.callsigns())));
        }
        return simulations.save(sim);
    }
}
