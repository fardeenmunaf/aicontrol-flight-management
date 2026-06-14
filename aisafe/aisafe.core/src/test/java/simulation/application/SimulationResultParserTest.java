package simulation.application;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimulationResultParserTest {

    private static final String JSON = """
        {
          "simulation": { "wallClockStart": "2026-06-22T05:30:00" },
          "safetyValidation": { "totalViolationsDetected": 1, "validationResult": "FAIL" },
          "flightExecutionStatuses": [
            { "callsign": "TAP0622A", "completed": true,  "hadViolation": false },
            { "callsign": "TAP0629X", "completed": false, "hadViolation": true }
          ],
          "violationEvents": [
            { "violationId": 0, "wallClockTime": "2026-06-29T09:10:00",
              "aircraftA": { "callsign": "TAP0629X" }, "aircraftB": { "callsign": "TAP0629X" } }
          ]
        }
        """;

    @Test
    void parsesFlightsAndViolations() {
        final SimulationResultDTO dto = new SimulationResultParser().parse(JSON);
        assertEquals(2, dto.flights().size());
        assertTrue(dto.flights().get(0).completed());
        assertTrue(dto.flights().get(1).hadViolation());
        assertEquals(1, dto.violations().size());
        assertEquals("2026-06-22T05:30:00Z", dto.startDateTime().toString());
    }
}
