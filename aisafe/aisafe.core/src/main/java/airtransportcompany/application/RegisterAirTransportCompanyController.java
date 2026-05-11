package airtransportcompany.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.strings.util.StringPredicates;

import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;

public class RegisterAirTransportCompanyController {

    private final AuthorizationService authz =
            AuthzRegistry.authorizationService();
    private final AirTransportCompanyRepository repository;

    public RegisterAirTransportCompanyController(final AirTransportCompanyRepository repository) {
        this.repository = repository;
    }

    public AirTransportCompany registerCompany(final String name,
                                               final String iataCode,
                                               final String icaoCode) {
        // AC01: Only an authenticated Backoffice Operator can register a company
        authz.ensureAuthenticatedUserHasAnyOf(Role.valueOf("BACKOFFICE_OPERATOR"));

        // AC04: Company name must be provided and non-empty
        if (StringPredicates.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Company name must be provided and non-empty");
        }

        // AC02: IATA code must be exactly 2 letters
        if (!isValidIataCode(iataCode)) {
            throw new IllegalArgumentException("IATA code must be exactly 2 letters");
        }

        // AC03: ICAO code must be 2–3 letters
        if (!isValidIcaoCode(icaoCode)) {
            throw new IllegalArgumentException("ICAO code must be 2–3 letters");
        }

        // AC05: Check for duplicate IATA code
        if (!repository.findByIataCode(iataCode).isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("A company with IATA code '%s' already exists", iataCode));
        }

        // AC05: Check for duplicate ICAO code
        if (!repository.findByIcaoCode(icaoCode).isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("A company with ICAO code '%s' already exists", icaoCode));
        }

        // Create and save the new company
        final var company = new AirTransportCompany(name, iataCode, icaoCode);
        return repository.save(company);
    }

    private boolean isValidIataCode(final String code) {
        if (StringPredicates.isNullOrEmpty(code)) {
            return false;
        }
        final String trimmed = code.trim().toUpperCase();
        return trimmed.length() == 2 && trimmed.matches("[A-Z]{2}");
    }

    private boolean isValidIcaoCode(final String code) {
        if (StringPredicates.isNullOrEmpty(code)) {
            return false;
        }
        final String trimmed = code.trim().toUpperCase();
        return trimmed.length() >= 2 && trimmed.length() <= 3 && trimmed.matches("[A-Z]{2,3}");
    }
}

