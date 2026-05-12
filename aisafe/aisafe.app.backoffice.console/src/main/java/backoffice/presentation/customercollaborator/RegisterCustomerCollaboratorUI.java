package backoffice.presentation.customercollaborator;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import shared.domain.UserId;
import user.domain.Role;
import user.domain.User;
import user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RegisterCustomerCollaboratorUI {

    private final UserRepository userRepository;
    private final CustomerCollaboratorRepository collaboratorRepository;
    private final AirTransportCompanyRepository airTransportCompanyRepository;
    private final AirControlAreaRepository airControlAreaRepository;

    public RegisterCustomerCollaboratorUI(final UserRepository userRepository,
                                          final CustomerCollaboratorRepository collaboratorRepository,
                                          final AirTransportCompanyRepository airTransportCompanyRepository,
                                          final AirControlAreaRepository airControlAreaRepository) {
        this.userRepository = userRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.airTransportCompanyRepository = airTransportCompanyRepository;
        this.airControlAreaRepository = airControlAreaRepository;
    }

    public boolean show() {
        final int type = Console.readInteger("Customer type (1-Air Transport Company, 2-Air Control Area): ");

        if (type == 1) {
            registerAirTransportCollaborator();
        } else if (type == 2) {
            registerAirControlCollaborator();
        } else {
            System.out.println("Invalid option.");
        }

        return true;
    }

    private void registerAirTransportCollaborator() {
        final List<AirTransportCompany> companies = toList(airTransportCompanyRepository.findAll());

        if (companies.isEmpty()) {
            System.out.println("No air transport companies available.");
            return;
        }

        for (int i = 0; i < companies.size(); i++) {
            System.out.printf("%d - %s%n", i + 1, companies.get(i));
        }

        final int choice = Console.readInteger("Choose one company: ");
        if (choice < 1 || choice > companies.size()) {
            System.out.println("Invalid option.");
            return;
        }

        final AirTransportCompany selected = companies.get(choice - 1);
        final String email = Console.readLine("Email: ");
        final String password = Console.readLine("Password: ");
        final String name = Console.readLine("Name: ");
        final String phone = Console.readLine("Phone number: ");
        final String position = Console.readLine("Position: ");

        if (userRepository.findByEmail(email).isPresent()) {
            System.out.println("Error: user already exists.");
            return;
        }

        final String[] split = splitName(name);

        try {
            AuthzRegistry.userService().registerNewUser(
                    email,
                    password,
                    split[0],
                    split[1],
                    email,
                    Set.of(auth.domain.AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)
            );
        } catch (Exception e) {
            System.out.println("Failed to create auth user: " + e.getMessage());
            return;
        }

        final User user = new User(
                email,
                name,
                phone,
                null,
                null,
                "ACTIVE",
                Set.of(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR),
                UserId.valueOf(email)
        );
        final User savedUser = userRepository.save(user);

        collaboratorRepository.save(new CustomerCollaborator(
                savedUser.identity(),
                CustomerType.AIR_TRANSPORT_COMPANY,
                selected.identity(),
                null,
                position
        ));

        System.out.println("Customer collaborator registered.");
    }

    private void registerAirControlCollaborator() {
        final List<AirControlArea> areas = toList(airControlAreaRepository.findAll());

        if (areas.isEmpty()) {
            System.out.println("No air control areas available.");
            return;
        }

        for (int i = 0; i < areas.size(); i++) {
            System.out.printf("%d - %s%n", i + 1, areas.get(i));
        }

        final int choice = Console.readInteger("Choose one air control area: ");
        if (choice < 1 || choice > areas.size()) {
            System.out.println("Invalid option.");
            return;
        }

        final AirControlArea selected = areas.get(choice - 1);
        final String email = Console.readLine("Email: ");
        final String password = Console.readLine("Password: ");
        final String name = Console.readLine("Name: ");
        final String phone = Console.readLine("Phone number: ");
        final String position = Console.readLine("Position: ");

        if (userRepository.findByEmail(email).isPresent()) {
            System.out.println("Error: user already exists.");
            return;
        }

        final String[] split = splitName(name);

        try {
            AuthzRegistry.userService().registerNewUser(
                    email,
                    password,
                    split[0],
                    split[1],
                    email,
                    Set.of(auth.domain.AISafeRoles.FLIGHT_CONTROL_OPERATOR)
            );
        } catch (Exception e) {
            System.out.println("Failed to create auth user: " + e.getMessage());
            return;
        }

        final User user = new User(
                email,
                name,
                phone,
                null,
                null,
                "ACTIVE",
                Set.of(Role.FLIGHT_CONTROL_OPERATOR),
                UserId.valueOf(email)
        );
        final User savedUser = userRepository.save(user);

        collaboratorRepository.save(new CustomerCollaborator(
                savedUser.identity(),
                CustomerType.AIR_CONTROL_AREA,
                null,
                selected.identity(),
                position
        ));

        System.out.println("Customer collaborator registered.");
    }

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        for (final T item : iterable) {
            list.add(item);
        }
        return list;
    }

    private String[] splitName(final String name) {
        final String trimmed = name == null ? "" : name.trim();
        final int idx = trimmed.indexOf(' ');
        if (idx <= 0 || idx == trimmed.length() - 1) {
            return new String[]{trimmed.isEmpty() ? "User" : trimmed, "User"};
        }
        return new String[]{trimmed.substring(0, idx), trimmed.substring(idx + 1)};
    }
}