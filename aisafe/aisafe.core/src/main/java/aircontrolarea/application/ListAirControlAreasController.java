package aircontrolarea.application;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.List;
import java.util.stream.StreamSupport;

public class ListAirControlAreasController {

    private final AuthorizationService authz =
            AuthzRegistry.authorizationService();
    private final AirControlAreaRepository repository;

    public ListAirControlAreasController(final AirControlAreaRepository repository) {
        this.repository = repository;
    }

    public List<AirControlArea> allAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return StreamSupport
                .stream(repository.findAll().spliterator(), false)
                .toList();
    }
}