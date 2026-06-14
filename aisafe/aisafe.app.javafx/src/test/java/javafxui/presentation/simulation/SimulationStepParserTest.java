package javafxui.presentation.simulation;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class SimulationStepParserTest {

    private final SimulationStepParser parser = new SimulationStepParser();

    @Test
    void parse_physics_row_returns_simstep() {
        final String line = "FR0016   CLIMB    53.4213   -6.2701    1200   300.5  10000      -   68513   13000";
        final Optional<SimStep> result = parser.parsePhysicsRow(line);
        assertTrue(result.isPresent());
        final SimStep step = result.get();
        assertEquals("FR0016", step.callsign());
        assertEquals("CLIMB",  step.phase());
        assertEquals(53.4213,  step.lat(),  0.0001);
        assertEquals(-6.2701,  step.lon(),  0.0001);
        assertEquals(1200.0,   step.alt(),  0.01);
        assertEquals(13000.0,  step.fuelRemaining(), 0.01);
    }

    @Test
    void parse_gate_row_dashes_for_fuel_uses_last_column() {
        final String line = "FR0013   GATE     48.7233    2.3794      89    479.0  8000      -        -         -      -       -     -       -       -      -         -        -         -        -        -       -       -   62013    7000";
        final Optional<SimStep> result = parser.parsePhysicsRow(line);
        assertTrue(result.isPresent());
        assertEquals("FR0013", result.get().callsign());
        assertEquals("GATE",   result.get().phase());
        assertEquals(7000.0,   result.get().fuelRemaining(), 0.01);
    }

    @Test
    void non_flight_line_returns_empty() {
        final String line = "Loaded 20 flight(s). Simulation: 0 s -> 34200 s";
        assertTrue(parser.parsePhysicsRow(line).isEmpty());
    }

    @Test
    void parse_violation_line_extracts_callsign() {
        final String line = "[FR0009] SIGUSR1: safety violation detected.";
        final Optional<String> result = parser.parseViolation(line);
        assertTrue(result.isPresent());
        assertEquals("FR0009", result.get());
    }

    @Test
    void non_violation_line_returns_empty() {
        final String line = "FR0009   CRUISE   52.0000  -1.0000  8000  100.0  8000  250  101325  1.225  340  270  0.82  220  0.3  0.05  500000  50000  80000  160000  2.0  5.0  150  67000  11800";
        assertTrue(parser.parseViolation(line).isEmpty());
    }

    @Test
    void parse_step_banner_extracts_step_number_active_violations() {
        final String line = "║  [05/06/2026 06:15]  |  step   1 / 38   |  active= 1  pending=18  |  violations=  0                                                      ║";
        final Optional<SimulationStepParser.StepBanner> result = parser.parseStepBanner(line);
        assertTrue(result.isPresent());
        assertEquals(1,   result.get().step());
        assertEquals(1,   result.get().active());
        assertEquals(18,  result.get().pending());
        assertEquals(0,   result.get().violations());
    }

    @Test
    void non_banner_line_returns_empty_banner() {
        final String line = "FR0016   CRUISE   52.0  -3.0  8000  200.0  8000  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  68200  12900";
        assertTrue(parser.parseStepBanner(line).isEmpty());
    }
}
