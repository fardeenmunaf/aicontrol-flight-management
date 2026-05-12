package backoffice.presentation.customercollaborator;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.application.RegisterCustomerCollaboratorController;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class RegisterCustomerCollaboratorUI extends AbstractUI {

    private final UserRepository userRepository =
            PersistenceContext.repositories().users();
    private final CustomerCollaboratorRepository collaboratorRepository =
            PersistenceContext.repositories().customerCollaborators();
    private final AirTransportCompanyRepository airTransportCompanyRepository =
            PersistenceContext.repositories().airTransportCompanies();
    private final AirControlAreaRepository airControlAreaRepository =
            PersistenceContext.repositories().airControlAreas();

    private final RegisterCustomerCollaboratorController controller =
            new RegisterCustomerCollaboratorController(
                    userRepository, collaboratorRepository,
                    airTransportCompanyRepository, airControlAreaRepository);

    @Override
    protected boolean doShow() {
        final int type = Console.readInteger("Customer type (1-Air Transport Company, 2-Air Control Area): ");

        if (type == 1) {
            registerAirTransportCollaborator();
        } else if (type == 2) {
            registerAirControlCollaborator();
        } else {
            System.out.println("Invalid option.");
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Customer Collaborator";
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
        final String password = Console.readLine("Password (min. 8 characters): ");
        final String name = Console.readLine("Name: ");
        final String phone = Console.readLine("Phone number: ");
        final String position = Console.readLine("Position: ");

        if (!validateInputs(email, password, name, phone, position)) {
            return;
        }

        try {
            controller.registerForAirTransportCompany(
                    selected.identity().toString(),
                    email, password, name, phone, position);
            System.out.println("Customer collaborator registered.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
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
        final String password = Console.readLine("Password (min. 8 characters): ");
        final String name = Console.readLine("Name: ");
        final String phone = Console.readLine("Phone number: ");
        final String position = Console.readLine("Position: ");

        if (!validateInputs(email, password, name, phone, position)) {
            return;
        }

        try {
            controller.registerForAirControlArea(
                    selected.identity().toString(),
                    email, password, name, phone, position);
            System.out.println("Customer collaborator registered.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private boolean validateInputs(final String email, final String password,
                                   final String name, final String phone,
                                   final String position) {
        if (isBlank(email)) {
            System.out.println("Error: email cannot be empty.");
            return false;
        }
        if (!isValidEmail(email)) {
            System.out.println("Error: invalid email format.");
            return false;
        }
        if (password == null || password.length() < 8) {
            System.out.println("Error: password must be at least 8 characters.");
            return false;
        }
        if (isBlank(name)) {
            System.out.println("Error: name cannot be empty.");
            return false;
        }
        if (isBlank(phone)) {
            System.out.println("Error: phone number cannot be empty.");
            return false;
        }
        if (!isValidPhone(phone)) {
            System.out.println("Error: phone number must contain only digits and may start with '+'.");
            return false;
        }
        if (isBlank(position)) {
            System.out.println("Error: position cannot be empty.");
            return false;
        }
        return true;
    }

    private boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidEmail(final String email) {
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private boolean isValidPhone(final String phone) {
        return phone.matches("^\\+?[0-9]{7,15}$");
    }

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        for (final T item : iterable) {
            list.add(item);
        }
        return list;
    }
}
