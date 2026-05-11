package customercollaborator.application;

import auth.domain.AISafeRoles;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.domain.model.Username;
import shared.domain.UserId;
import user.domain.User;
import user.repository.UserRepository;

public class DisableCustomerCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final CustomerCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    public DisableCustomerCollaboratorController(final CustomerCollaboratorRepository collaboratorRepository,
                                                 final UserRepository userRepository) {
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
    }

    public void disable(final String userId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final UserId id = UserId.valueOf(userId);

        final CustomerCollaborator collaborator = collaboratorRepository.findByUserId(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer collaborator does not exist"));

        final User user = userRepository.ofIdentity(id)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));

        collaborator.disable();
        collaboratorRepository.save(collaborator);

        AuthzRegistry.userService()
                .userOfIdentity(Username.valueOf(user.email()))
                .ifPresent(AuthzRegistry.userService()::deactivateUser);

        user.disable();
        userRepository.save(user);
    }
}