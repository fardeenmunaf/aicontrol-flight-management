package airtransportcompany.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AirTransportCompanyTest {

    private AirTransportCompany validCompany() {
        return new AirTransportCompany("Test Airlines", "TA", "TAL");
    }

    // --- Construction: happy path ---

    @Test
    void ensureCompanyIsCreatedWithExpectedState() {
        final AirTransportCompany company = validCompany();
        assertAll(
                () -> assertEquals("Test Airlines", company.name()),
                () -> assertEquals("TA", company.iataCode()),
                () -> assertEquals("TAL", company.icaoCode()),
                () -> assertNotNull(company.identity())
        );
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullNameIsRejected() {
        assertThrows(Exception.class, () -> new AirTransportCompany(null, "TA", "TAL"));
    }

    @Test
    void ensureEmptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirTransportCompany("", "TA", "TAL"));
    }

    @Test
    void ensureNullIataCodeIsRejected() {
        assertThrows(Exception.class, () -> new AirTransportCompany("Test Airlines", null, "TAL"));
    }

    @Test
    void ensureEmptyIataCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirTransportCompany("Test Airlines", "", "TAL"));
    }

    @Test
    void ensureNullIcaoCodeIsRejected() {
        assertThrows(Exception.class, () -> new AirTransportCompany("Test Airlines", "TA", null));
    }

    @Test
    void ensureEmptyIcaoCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirTransportCompany("Test Airlines", "TA", ""));
    }

    // --- equals / hashCode ---

    @Test
    void ensureCompanyIsEqualToItself() {
        final AirTransportCompany company = validCompany();
        assertEquals(company, company);
        assertEquals(company.hashCode(), company.hashCode());
    }

    @Test
    void ensureCompanyIsNotEqualToNull() {
        assertNotEquals(validCompany(), null);
    }

    @Test
    void ensureTwoDifferentCompaniesAreNotEqual() {
        final AirTransportCompany c1 = validCompany();
        final AirTransportCompany c2 = new AirTransportCompany("Other Airlines", "OA", "OAL");
        assertNotEquals(c1, c2);
    }

    // --- toString ---

    @Test
    void ensureToStringContainsNameAndCodes() {
        final AirTransportCompany company = validCompany();
        final String str = company.toString();
        assertTrue(str.contains("Test Airlines"));
        assertTrue(str.contains("TA"));
        assertTrue(str.contains("TAL"));
    }

    @Test
    void ensureCompanySameAsItself() {
        final AirTransportCompany company = validCompany();
        assertTrue(company.sameAs(company));
    }

    @Test
    void ensureCompanySameAsNullIsFalse() {
        final AirTransportCompany company = validCompany();
        assertFalse(company.sameAs(null));
    }
}
