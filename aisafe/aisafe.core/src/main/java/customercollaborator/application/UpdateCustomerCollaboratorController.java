package customercollaborator.application;

import auth.domain.AISafeRoles;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import shared.domain.UserId;
import user.domain.User;
import user.repository.UserRepository;

/**
 * Controller for updating a customer collaborator's contact information (email and phone number).
 * Only email and phone number can be updated through this operation.
 */
public class UpdateCustomerCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final CustomerCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    public UpdateCustomerCollaboratorController(final CustomerCollaboratorRepository collaboratorRepository,
                                               final UserRepository userRepository) {
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
    }

    /**
     * Updates the email and/or phone number of a customer collaborator.
     * Only authenticated Backoffice Operators can perform this action.
     *
     * @param userId the user ID of the collaborator to update
     * @param newEmail the new email address (or null to keep current)
     * @param newPhoneNumber the new phone number (or null to keep current)
     * @return the updated user information
     * @throws IllegalArgumentException if collaborator or user not found, or if validation fails
     * @throws IllegalStateException if user is not authenticated or not authorized
     */
    public User updateCollaboratorContactInfo(final String userId,
                                              final String newEmail,
                                              final String newPhoneNumber) {
        // AC01: Only authenticated Backoffice Operators can edit
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final UserId id = UserId.valueOf(userId);

        // AC06: Check if collaborator exists
        final CustomerCollaborator collaborator = collaboratorRepository.findByUserId(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer collaborator does not exist"));

        // AC06: Check if collaborator is disabled
        if (!collaborator.isActive()) {
            throw new IllegalArgumentException("Customer collaborator is disabled");
        }

        // Get the user
        final User user = userRepository.ofIdentity(id)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));

        // AC02: Only email and phone number can be edited
        // Update email if provided
        if (newEmail != null && !newEmail.isBlank()) {
            // AC03: Validate email format
            user.updateEmail(newEmail);
        }

        // Update phone if provided
        if (newPhoneNumber != null && !newPhoneNumber.isBlank()) {
            // AC04: Validate phone number format
            user.updatePhoneNumber(newPhoneNumber);
        }

        // Save the updated user
        userRepository.save(user);

        return user;
    }
}
