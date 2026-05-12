package backoffice.presentation.customercollaborator;

import customercollaborator.application.UpdateCustomerCollaboratorController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import user.domain.User;

/**
 * UI for updating a customer collaborator's contact information.
 * Allows Backoffice Operators to edit email and phone number.
 */
public class UpdateCustomerCollaboratorUI extends AbstractUI {

    private final UpdateCustomerCollaboratorController controller =
            new UpdateCustomerCollaboratorController(
                    PersistenceContext.repositories().customerCollaborators(),
                    PersistenceContext.repositories().users()
            );

    @Override
    protected boolean doShow() {
        try {
            final String userId = Console.readLine("Collaborator User ID");
            
            System.out.println("\nLeave empty to keep the current value:");
            final String newEmail = Console.readLine("New Email (optional)");
            final String newPhoneNumber = Console.readLine("New Phone Number (optional)");

            if (newEmail.isBlank() && newPhoneNumber.isBlank()) {
                System.out.println("At least one field must be provided for update.");
                return false;
            }

            final User updatedUser = controller.updateCollaboratorContactInfo(
                    userId,
                    newEmail.isBlank() ? null : newEmail,
                    newPhoneNumber.isBlank() ? null : newPhoneNumber
            );

            System.out.println("\nCustomer collaborator information successfully updated.");
            System.out.println("Email: " + updatedUser.email());
            System.out.println("Phone: " + updatedUser.phoneNumber());
            
            return true;
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        } catch (final Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Update Customer Collaborator Contact Information";
    }
}
