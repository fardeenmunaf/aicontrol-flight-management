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
                             final Role domainRole) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN);

        final var nameParts = splitName(name);
        AuthzRegistry.userService().registerNewUser(
                email,
                rawPassword,
                nameParts[0],
                nameParts[1],
                email,
                Set.of(toAuthzRole(domainRole))
        );

        // Domain User — handles business data
        final var domainUser = new User(
                email, name, phoneNumber,
                null, null, "ACTIVE",
                Set.of(domainRole),
                UserId.valueOf(email)
        );
        return userRepository.save(domainUser);
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