package customercollaborator.application;

import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import auth.domain.AISafeRoles;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;
import user.domain.Role;
import user.domain.User;
import user.repository.UserRepository;

import java.util.Set;

public class RegisterCustomerCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserRepository userRepository;
    private final CustomerCollaboratorRepository collaboratorRepository;
    private final AirTransportCompanyRepository airTransportCompanyRepository;
    private final AirControlAreaRepository airControlAreaRepository;

    public RegisterCustomerCollaboratorController(final UserRepository userRepository,
                                                  final CustomerCollaboratorRepository collaboratorRepository,
                                                  final AirTransportCompanyRepository airTransportCompanyRepository,
                                                  final AirControlAreaRepository airControlAreaRepository) {
        this.userRepository = userRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.airTransportCompanyRepository = airTransportCompanyRepository;
        this.airControlAreaRepository = airControlAreaRepository;
    }

    public CustomerCollaborator registerForAirTransportCompany(final String companyId,
                                                               final String email,
                                                               final String rawPassword,
                                                               final String name,
                                                               final String phoneNumber,
                                                               final String position) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        validateCommonFields(companyId, email, rawPassword, name, phoneNumber, position);

        final AirTransportCompanyId id = AirTransportCompanyId.valueOf(companyId);
        airTransportCompanyRepository.ofIdentity(id)
                .orElseThrow(() -> new IllegalArgumentException("Air transport company does not exist"));

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("There is already a user with that email");
        }

        registerSystemUser(email, rawPassword, name, Role.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final User domainUser = new User(
                email, name, phoneNumber, null, null, "ACTIVE",
                Set.of(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR),
                UserId.valueOf(email)
        );
        final User savedUser = userRepository.save(domainUser);

        final CustomerCollaborator collaborator = new CustomerCollaborator(
                savedUser.identity(),
                CustomerType.AIR_TRANSPORT_COMPANY,
                id,
                null,
                position
        );
        return collaboratorRepository.save(collaborator);
    }

    public CustomerCollaborator registerForAirControlArea(final String areaId,
                                                          final String email,
                                                          final String rawPassword,
                                                          final String name,
                                                          final String phoneNumber,
                                                          final String position) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        validateCommonFields(areaId, email, rawPassword, name, phoneNumber, position);

        final AirControlAreaId id = AirControlAreaId.valueOf(areaId);
        airControlAreaRepository.ofIdentity(id)
                .orElseThrow(() -> new IllegalArgumentException("Air control area does not exist"));

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("There is already a user with that email");
        }

        registerSystemUser(email, rawPassword, name, Role.FLIGHT_CONTROL_OPERATOR);

        final User domainUser = new User(
                email, name, phoneNumber, null, null, "ACTIVE",
                Set.of(Role.FLIGHT_CONTROL_OPERATOR),
                UserId.valueOf(email)
        );
        final User savedUser = userRepository.save(domainUser);

        final CustomerCollaborator collaborator = new CustomerCollaborator(
                savedUser.identity(),
                CustomerType.AIR_CONTROL_AREA,
                null,
                id,
                position
        );
        return collaboratorRepository.save(collaborator);
    }

    private void registerSystemUser(final String email, final String rawPassword,
                                    final String name, final Role role) {
        final var parts = splitName(name);
        AuthzRegistry.userService().registerNewUser(
                email,
                rawPassword,
                parts[0],
                parts[1],
                email,
                Set.of(toAuthzRole(role))
        );
    }

    private String[] splitName(final String name) {
        final var trimmed = name == null ? "" : name.trim();
        final var spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex <= 0 || spaceIndex == trimmed.length() - 1) {
            return new String[]{trimmed.isEmpty() ? "User" : trimmed, "User"};
        }
        return new String[]{trimmed.substring(0, spaceIndex), trimmed.substring(spaceIndex + 1)};
    }

    private eapli.framework.infrastructure.authz.domain.model.Role toAuthzRole(final Role domainRole) {
        return switch (domainRole) {
            case ADMIN -> AISafeRoles.ADMIN;
            case BACKOFFICE_OPERATOR -> AISafeRoles.BACKOFFICE_OPERATOR;
            case AIR_TRANSPORT_COMPANY_COLLABORATOR -> AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR;
            case PILOT -> AISafeRoles.PILOT;
            case FLIGHT_CONTROL_OPERATOR -> AISafeRoles.FLIGHT_CONTROL_OPERATOR;
            case WEATHER_PERSON -> AISafeRoles.WEATHER_PERSON;
        };
    }

    private void validateCommonFields(final String customerId,
                                      final String email,
                                      final String rawPassword,
                                      final String name,
                                      final String phoneNumber,
                                      final String position) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer id cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException("Position cannot be null or empty");
        }
    }
}