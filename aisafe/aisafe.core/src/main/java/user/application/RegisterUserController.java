package user.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import auth.domain.AISafeRoles;
import shared.domain.UserId;
import user.domain.Role;
import user.domain.User;
import user.repository.UserRepository;

import java.util.Set;

public class RegisterUserController {

    private final AuthorizationService authz =
            AuthzRegistry.authorizationService();
    private final UserRepository userRepository;

    public RegisterUserController(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(final String email, final String rawPassword,
                             final String name, final String phoneNumber,
                             final Set<Role> domainRoles) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN);

        final var nameParts = splitName(name);
        // Convert domain roles to authz roles
        Set<eapli.framework.infrastructure.authz.domain.model.Role> authzRoles = domainRoles.stream()
                .map(this::toAuthzRole)
                .collect(java.util.stream.Collectors.toSet());
        AuthzRegistry.userService().registerNewUser(
                email,
                rawPassword,
                nameParts[0],
                nameParts[1],
                email,
                authzRoles
        );

        // Domain User — handles business data
        final var domainUser = new User(
                email, name, phoneNumber,
                null, null, "ACTIVE",
                domainRoles,
                UserId.valueOf(email)
        );
        return userRepository.save(domainUser);
    }

    /**
     * Edit a user's phone number and roles (admin only). Cannot change email, password, or name.
     * @param email User's email (identifier)
     * @param newPhoneNumber New phone number
     * @param newRoles New set of roles
     * @return Updated User
     */
    public User editUser(final String email, final String newPhoneNumber, final Set<Role> newRoles) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN);
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        var user = userOpt.get();
        // Create a new User instance with updated phone and roles, keeping other fields
        var updatedUser = new User(
                user.email(),
                user.name(),
                newPhoneNumber,
                user.activeSecurityClearanceExpiry(),
                user.skillsAssessmentDueDate(),
                user.status(),
                newRoles,
                user.userId()
        );
        return userRepository.save(updatedUser);
    }

    private String[] splitName(final String name) {
        final var trimmed = name == null ? "" : name.trim();
        final var spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex <= 0 || spaceIndex == trimmed.length() - 1) {
            return new String[] { trimmed.isEmpty() ? "User" : trimmed, "User" };
        }
        return new String[] { trimmed.substring(0, spaceIndex), trimmed.substring(spaceIndex + 1) };
    }

    private eapli.framework.infrastructure.authz.domain.model.Role toAuthzRole(
            final Role domainRole) {
        return switch (domainRole) {
            case ADMIN                              -> AISafeRoles.ADMIN;
            case BACKOFFICE_OPERATOR                -> AISafeRoles.BACKOFFICE_OPERATOR;
            case AIR_TRANSPORT_COMPANY_COLLABORATOR -> AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR;
            case PILOT                              -> AISafeRoles.PILOT;
            case FLIGHT_CONTROL_OPERATOR            -> AISafeRoles.FLIGHT_CONTROL_OPERATOR;
            case WEATHER_PERSON                     -> AISafeRoles.WEATHER_PERSON;
        };
    }
}