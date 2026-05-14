package airport.application;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airport.domain.Airport;
import airport.domain.AirportCoordinates;
import airport.repositories.AirportRepository;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import shared.domain.AirControlAreaId;

import java.util.Optional;

public class RegisterAirportController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AirportRepository airportRepository;
    private final AirControlAreaRepository airControlAreaRepository;

    public RegisterAirportController(
            final AirportRepository airportRepository,
            final AirControlAreaRepository airControlAreaRepository) {
        this.airportRepository = airportRepository;
        this.airControlAreaRepository = airControlAreaRepository;
    }

    public Optional<AirControlArea> findAreaForCoordinates(final double latitude, final double longitude) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return airControlAreaRepository.findContaining(latitude, longitude);
    }

    public Airport registerAirport(
            final String name,
            final String town,
            final String country,
            final String iataCode,
            final String icaoCode,
            final Double elevation,
            final Double latitude,
            final Double longitude,
            final String airControlAreaId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Airport name cannot be null or empty");
        }
        if (town == null || town.trim().isEmpty()) {
            throw new IllegalArgumentException("Airport town cannot be null or empty");
        }
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Airport country cannot be null or empty");
        }
        if (airControlAreaId == null || airControlAreaId.trim().isEmpty()) {
            throw new IllegalArgumentException("Air control area id must be specified");
        }

        if (iataCode == null || iataCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Airport IATA code cannot be null or empty");
        }
        if (icaoCode == null || icaoCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Airport ICAO code cannot be null or empty");
        }
        if (elevation == null) {
            throw new IllegalArgumentException("Airport elevation must be specified");
        }
        if (latitude == null) {
            throw new IllegalArgumentException("Latitude must be specified");
        }
        if (longitude == null) {
            throw new IllegalArgumentException("Longitude must be specified");
        }

        final String normalizedIata = iataCode.trim().toUpperCase();
        final String normalizedIcao = icaoCode.trim().toUpperCase();

        if (!normalizedIata.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Airport IATA code must have exactly 3 letters");
        }

        if (!normalizedIcao.matches("[A-Z]{4}")) {
            throw new IllegalArgumentException("Airport ICAO code must have exactly 4 letters");
        }

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        if (airportRepository.findByIataCode(normalizedIata).isPresent()) {
            throw new IllegalArgumentException("Airport with IATA code '" + normalizedIata + "' already exists");
        }
        if (airportRepository.findByIcaoCode(normalizedIcao).isPresent()) {
            throw new IllegalArgumentException("Airport with ICAO code '" + normalizedIcao + "' already exists");
        }

        final AirControlArea area = airControlAreaRepository
                .ofIdentity(AirControlAreaId.valueOf(airControlAreaId))
                .orElseThrow(() -> new IllegalArgumentException("Air control area does not exist"));

        final Airport airport = new Airport(
                name,
                town,
                country,
                normalizedIata,
                normalizedIcao,
                elevation,
                new AirportCoordinates(latitude, longitude),
                area
        );

        return airportRepository.save(airport);
    }
}