package backoffice.presentation.customercollaborator;

import customercollaborator.application.ListCustomerCollaboratorsController;
import customercollaborator.domain.CustomerCollaborator;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

public class ListCustomerCollaboratorsUI extends AbstractUI {

    private final ListCustomerCollaboratorsController controller =
            new ListCustomerCollaboratorsController(
                    PersistenceContext.repositories().customerCollaborators()
            );

    @Override
    protected boolean doShow() {
        try {
            final int option = Console.readInteger("Customer type (1-Air Transport Company, 2-Air Control Area)");
            final String customerId = Console.readLine("Customer id");

            final Iterable<CustomerCollaborator> collaborators =
                    option == 1
                            ? controller.listActiveByAirTransportCompany(customerId)
                            : controller.listActiveByAirControlArea(customerId);

            for (final CustomerCollaborator collaborator : collaborators) {
                System.out.println("UserId: " + collaborator.userId()
                        + " | Position: " + collaborator.position()
                        + " | Active: " + collaborator.isActive());
            }

            return true;
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "List Customer Collaborators";
    }
}