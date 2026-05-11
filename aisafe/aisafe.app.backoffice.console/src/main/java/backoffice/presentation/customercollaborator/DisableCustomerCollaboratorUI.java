package backoffice.presentation.customercollaborator;

import customercollaborator.application.DisableCustomerCollaboratorController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

public class DisableCustomerCollaboratorUI extends AbstractUI {

    private final DisableCustomerCollaboratorController controller =
            new DisableCustomerCollaboratorController(
                    PersistenceContext.repositories().customerCollaborators(),
                    PersistenceContext.repositories().users()
            );

    @Override
    protected boolean doShow() {
        try {
            final String userId = Console.readLine("User id");
            controller.disable(userId);
            System.out.println("Customer collaborator successfully disabled.");
            return true;
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Disable Customer Collaborator";
    }
}