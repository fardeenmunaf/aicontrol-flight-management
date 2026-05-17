package bootstrap;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Username;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;
import user.domain.Role;
import user.domain.User;
import user.repository.UserRepository;

import java.util.List;
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
        // Air Transport Company Collaborators
        registerAirTransportCollaboratorIfAbsent("atcc1@tap.pt", "Password123", "ATCC One", "910000001", "TP", "Operations Manager");
        registerAirTransportCollaboratorIfAbsent("atcc1@ryanair.ie", "Password123", "ATCC RyanAir", "911000001", "FR", "Fleet Manager");
        
        // Air Control Area Collaborators
        registerAirControlAreaCollaboratorIfAbsent("fco1@aisafe.pt", "Password123", "FCO One", "920000001", "LPPC", "Area Supervisor");
    }

    private void registerAirTransportCollaboratorIfAbsent(final String email,
                                                           final String password,
                                                           final String name,
                                                           final String phoneNumber,
                                                           final String iataCode,
                                                           final String position) {
        if (AuthzRegistry.userService().userOfIdentity(Username.valueOf(email)).isPresent()) {
            return;
        }

        final List<AirTransportCompany> companies = airTransportCompanyRepository.findByIataCode(iataCode);
        if (companies.isEmpty()) {
            System.out.println("Air Transport Company with IATA code " + iataCode + " not found. Skipping collaborator " + email);
            return;
        }

        final AirTransportCompanyId companyId = companies.get(0).identity();
        registerUserAndCollaborator(
                email,
                password,
                name,
                phoneNumber,
                Role.AIR_TRANSPORT_COMPANY_COLLABORATOR,
                CustomerType.AIR_TRANSPORT_COMPANY,
                companyId,
                null,
                position
        );
    }

    private void registerAirControlAreaCollaboratorIfAbsent(final String email,
                                                             final String password,
                                                             final String name,
                                                             final String phoneNumber,
                                                             final String areaCode,
                                                             final String position) {
        if (AuthzRegistry.userService().userOfIdentity(Username.valueOf(email)).isPresent()) {
            return;
        }

        final List<AirControlArea> areas = airControlAreaRepository.findByAreaCode(areaCode);
        if (areas.isEmpty()) {
            System.out.println("Air Control Area with code " + areaCode + " not found. Skipping collaborator " + email);
            return;
        }

        final AirControlAreaId areaId = areas.get(0).identity();
        registerUserAndCollaborator(
                email,
                password,
                name,
                phoneNumber,
                Role.FLIGHT_CONTROL_OPERATOR,
                CustomerType.AIR_CONTROL_AREA,
                null,
                areaId,
                position
        );
    }

    private void registerUserAndCollaborator(final String email,
                                             final String rawPassword,
                                             final String name,
                                             final String phoneNumber,
                                             final Role domainRole,
                                             final CustomerType customerType,
                                             final AirTransportCompanyId airTransportCompanyId,
                                             final AirControlAreaId airControlAreaId,
                                             final String position) {
        final String[] parts = splitName(name);

        try {
            AuthzRegistry.userService().registerNewUser(
                    email,
                    rawPassword,
                    parts[0],
                    parts[1],
                    email,
                    Set.of(toAuthzRole(domainRole))
            );
        } catch (Exception e) {
            System.out.println("Failed to create auth user for " + email + ": " + e.getMessage());
            return;
        }

        final User user = new User(
                email,
                name,
                phoneNumber,
                null,
                null,
                "ACTIVE",
                Set.of(domainRole),
                UserId.valueOf(email)
        );

        final UserId userId = userRepository.save(user).identity();

        final CustomerCollaborator collaborator = new CustomerCollaborator(
                userId,
                customerType,
                airTransportCompanyId,
                airControlAreaId,
                position
        );

        collaboratorRepository.save(collaborator);
        System.out.println("Customer collaborator created: " + email);
    }

    private String[] splitName(final String name) {
        final String trimmed = name == null ? "" : name.trim();
        final int idx = trimmed.indexOf(' ');
        if (idx <= 0 || idx == trimmed.length() - 1) {
            return new String[]{trimmed.isEmpty() ? "User" : trimmed, "User"};
        }
        return new String[]{trimmed.substring(0, idx), trimmed.substring(idx + 1)};
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