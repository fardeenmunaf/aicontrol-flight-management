package aircontrolarea.application;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.strings.util.StringPredicates;

public class RegisterAirControlAreaController {

    private final AuthorizationService authz =
            AuthzRegistry.authorizationService();
    private final AirControlAreaRepository repository;

    public RegisterAirControlAreaController(final AirControlAreaRepository repository) {
        this.repository = repository;
    }

    public AirControlArea registerAirControlArea(final String areaCode,
                                                 final double minimumRequiredArrivalFuel,
                                                 final String geographicBoundaries) {
        // AC01: Only an authenticated Backoffice Operator can register an air control area
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        // AC02 / AC04: Area code must be unique
        if (StringPredicates.isNullOrEmpty(areaCode)) {
            throw new IllegalArgumentException("Area code must be provided and non-empty");
        }
        if (!repository.findByAreaCode(areaCode).isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("An air control area with code '%s' already exists", areaCode));
        }

        // AC03 / AC05: Geographic boundaries must be valid
        if (!isValidGeographicBoundaries(geographicBoundaries)) {
            throw new IllegalArgumentException(
                    "Geographic boundaries are invalid. Expected format: lat,lon;lat,lon;... "
                            + "Latitude must be in [-90, 90] and longitude in [-180, 180]");
        }

        final var area = new AirControlArea(areaCode, minimumRequiredArrivalFuel, geographicBoundaries);
        return repository.save(area);
    }

    /**
     * Validates geographic boundaries string.
     * Expected format: "lat1,lon1;lat2,lon2;..."
     * Latitude must be in [-90, 90], longitude in [-180, 180].
     */
    private boolean isValidGeographicBoundaries(final String geographicBoundaries) {
        if (StringPredicates.isNullOrEmpty(geographicBoundaries)) {
            return false;
        }
        final String[] points = geographicBoundaries.trim().split(";");
        if (points.length < 1) {
            return false;
        }
        for (final String point : points) {
            final String[] coords = point.trim().split(",");
            if (coords.length != 2) {
                return false;
            }
            try {
                final double latitude = Double.parseDouble(coords[0].trim());
                final double longitude = Double.parseDouble(coords[1].trim());
                if (latitude < -90 || latitude > 90) {
                    return false;
                }
                if (longitude < -180 || longitude > 180) {
                    return false;
                }
            } catch (final NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
}