package airtransport.presentation;

import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import aircraft.application.ListAircraftController;
import airtransportcompany.domain.AirTransportCompany;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import infrastructure.persistence.PersistenceContext;
import shared.domain.UserId;

public class ListAircraftUI extends AbstractUI {

    private final ListAircraftController controller = new ListAircraftController(PersistenceContext.repositories().aircraft());

    @Override
    protected boolean doShow() {
        try {
            final var session = AuthzRegistry.authorizationService().session().orElseThrow(IllegalStateException::new);
            final var userId = UserId.valueOf(session.authenticatedUser().identity().toString());
            final CustomerCollaborator collaborator = PersistenceContext.repositories().customerCollaborators()
                    .findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Current user is not a customer collaborator"));

            if (collaborator.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
                System.out.println("You are not assigned to an air transport company.");
                return true;
            }

            final AirTransportCompany company = PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(collaborator.airTransportCompanyId())
                    .orElseThrow(() -> new IllegalStateException("Air transport company not found"));

            System.out.println("Fleet for: " + company.name());
            System.out.println();

            controller.listAll().forEach(a -> {
                if (a.airTransportCompany() != null && a.airTransportCompany().identity().equals(company.identity())) {
                    System.out.println("Registration: " + a.registrationNumber() + " | Country: " + a.registrationCountry() + " | Status: " + a.operationStatus());
                }
            });
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() {
        return "List Aircraft";
    }
}
