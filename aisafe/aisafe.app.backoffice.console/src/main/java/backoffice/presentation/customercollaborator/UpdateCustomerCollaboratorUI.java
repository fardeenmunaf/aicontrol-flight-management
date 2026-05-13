package backoffice.presentation.customercollaborator;

import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.application.UpdateCustomerCollaboratorController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import shared.domain.UserId;
import user.domain.User;

import java.util.ArrayList;
import java.util.List;

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
            showAvailableCollaborators();

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

    private void showAvailableCollaborators() {
        final List<CustomerCollaborator> collaborators = toList(
                PersistenceContext.repositories().customerCollaborators().findAll());

        if (collaborators.isEmpty()) {
            System.out.println("No customer collaborators available.");
            return;
        }

        System.out.println("\nAvailable collaborators:");
        for (final CustomerCollaborator collaborator : collaborators) {
            final String collaboratorInfo = describeCollaborator(collaborator);
            System.out.printf("- User ID: %s | %s%n", collaborator.userId(), collaboratorInfo);
        }
        System.out.println();
    }

    private String describeCollaborator(final CustomerCollaborator collaborator) {
        final UserId userId = collaborator.userId();
        final String userInfo = PersistenceContext.repositories().users()
                .ofIdentity(userId)
                .map(User::name)
                .orElse("Unknown user");

        final String affiliation;
        if (collaborator.airTransportCompanyId() != null) {
            affiliation = "Air Transport Company: " + collaborator.airTransportCompanyId();
        } else if (collaborator.airControlAreaId() != null) {
            affiliation = "Air Control Area: " + collaborator.airControlAreaId();
        } else {
            affiliation = "No affiliation";
        }

        return String.format("Name: %s | Type: %s | %s | Position: %s | %s",
                userInfo,
                collaborator.customerType(),
                affiliation,
                collaborator.position(),
                collaborator.isActive() ? "ACTIVE" : "DISABLED");
    }

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        for (final T item : iterable) {
            list.add(item);
        }
        return list;
    }
}
