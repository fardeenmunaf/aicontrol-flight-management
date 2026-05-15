package aircraft.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AircraftDTOTest {

    @Test
    void ensureAircraftDTOIsCreatedWithExpectedState() {
        final AircraftDTO dto = new AircraftDTO(
                "CS-TKA",
                "B737",
                "IN_SERVICE",
                "Economy: 150, Business: 20, First: 10",
                "Portugal"
        );
        assertAll(
                () -> assertEquals("CS-TKA", dto.registrationNumber),
                () -> assertEquals("B737", dto.model),
                () -> assertEquals("IN_SERVICE", dto.operationalStatus),
                () -> assertTrue(dto.cabinConfig.contains("150")),
                () -> assertEquals("Portugal", dto.registrationCountry)
        );
    }

    @Test
    void ensureToStringContainsRegistrationNumber() {
        final AircraftDTO dto = new AircraftDTO("CS-TKA", "B737", "IN_SERVICE", "cabin", "Portugal");
        final String str = dto.toString();
        assertTrue(str.contains("CS-TKA"));
        assertTrue(str.contains("B737"));
        assertTrue(str.contains("IN_SERVICE"));
    }
}
