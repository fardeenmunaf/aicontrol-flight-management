package bootstrap;

import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;
import user.domain.Role;
import user.domain.User;
import user.repository.UserRepository;

import java.util.Set;

public class CustomerCollaboratorBootstrap {

    private final UserRepository userRepository;
    private final CustomerCollaboratorRepository collaboratorRepository;
    private final AirTransportCompanyRepository airTransportCompanyRepository;
    private final AirControlAreaRepository airControlAreaRepository;

    public CustomerCollaboratorBootstrap(final UserRepository userRepository,
                                         final CustomerCollaboratorRepository collaboratorRepository,
                                         final AirTransportCompanyRepository airTransportCompanyRepository,
                                         final AirControlAreaRepository airControlAreaRepository) {
        this.userRepository = userRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.airTransportCompanyRepository = airTransportCompanyRepository;
        this.airControlAreaRepository = airControlAreaRepository;
    }

    public void execute() {
        registerAirTransportCompanyCollaborator();
        registerAirControlAreaCollaborator();
    }

    private void registerAirTransportCompanyCollaborator() {
        final String companyId = "TAP-ID";
        final String email = "atcc1@tap.pt";

        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        final AirTransportCompanyId id = AirTransportCompanyId.valueOf(companyId);
        if (airTransportCompanyRepository.ofIdentity(id).isEmpty()) {
            return;
        }

        registerSystemUser(email, "Password123", "ATCC One", Role.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final User domainUser = userRepository.save(new User(
                email, "ATCC One", "910000001", null, null, "ACTIVE",
                Set.of(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR),
                UserId.valueOf(email)
        ));

        collaboratorRepository.save(new CustomerCollaborator(
                domainUser.identity(),
                CustomerType.AIR_TRANSPORT_COMPANY,
                id,
                null,
                "Operations Manager"
        ));
    }

    private void registerAirControlAreaCollaborator() {
        final String areaId = "PT-NORTH";
        final String email = "fco1@aisafe.pt";

        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        final AirControlAreaId id = AirControlAreaId.valueOf(areaId);
        if (airControlAreaRepository.ofIdentity(id).isEmpty()) {
            return;
        }

        registerSystemUser(email, "Password123", "FCO One", Role.FLIGHT_CONTROL_OPERATOR);

        final User domainUser = userRepository.save(new User(
                email, "FCO One", "920000001", null, null, "ACTIVE",
                Set.of(Role.FLIGHT_CONTROL_OPERATOR),
                UserId.valueOf(email)
        ));

        collaboratorRepository.save(new CustomerCollaborator(
                domainUser.identity(),
                CustomerType.AIR_CONTROL_AREA,
                null,
                id,
                "Area Supervisor"
        ));
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
            case ADMIN -> auth.domain.AISafeRoles.ADMIN;
            case BACKOFFICE_OPERATOR -> auth.domain.AISafeRoles.BACKOFFICE_OPERATOR;
            case AIR_TRANSPORT_COMPANY_COLLABORATOR -> auth.domain.AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR;
            case PILOT -> auth.domain.AISafeRoles.PILOT;
            case FLIGHT_CONTROL_OPERATOR -> auth.domain.AISafeRoles.FLIGHT_CONTROL_OPERATOR;
            case WEATHER_PERSON -> auth.domain.AISafeRoles.WEATHER_PERSON;
        };
    }
}