package airtransportcompany.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Role;

import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;

import java.util.List;
import java.util.stream.StreamSupport;

public class ListAirTransportCompaniesController {

    private final AuthorizationService authz =
            AuthzRegistry.authorizationService();
    private final AirTransportCompanyRepository repository;

    public ListAirTransportCompaniesController(final AirTransportCompanyRepository repository) {
        this.repository = repository;
    }

    public List<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(Role.valueOf("BACKOFFICE_OPERATOR"));
        return StreamSupport
                .stream(repository.findAll().spliterator(), false)
                .toList();
    }
}

