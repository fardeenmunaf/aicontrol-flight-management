package backoffice.presentation.customercollaborator;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.application.UpdateCustomerCollaboratorController;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import user.domain.User;
import user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class UpdateCustomerCollaboratorUI extends AbstractUI {

    private final CustomerCollaboratorRepository collaboratorRepository =
            PersistenceContext.repositories().customerCollaborators();
    private final AirTransportCompanyRepository airTransportCompanyRepository =
            PersistenceContext.repositories().airTransportCompanies();
    private final AirControlAreaRepository airControlAreaRepository =
            PersistenceContext.repositories().airControlAreas();
    private final UserRepository userRepository =
            PersistenceContext.repositories().users();

    private final UpdateCustomerCollaboratorController controller =
            new UpdateCustomerCollaboratorController(collaboratorRepository, userRepository);

    @Override
    protected boolean doShow() {
        final int type = Console.readInteger("Customer type (1-Air Transport Company, 2-Air Control Area): ");

        if (type == 1) {
            updateFromAirTransportCompany();
        } else if (type == 2) {
            updateFromAirControlArea();
        } else {
            System.out.println("Invalid option.");
        }

        return false;
    }

    @Override
    public String headline() {
        return "Update Customer Collaborator Contact Information";
    }

    private void updateFromAirTransportCompany() {
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
        final List<CustomerCollaborator> collaborators = toList(
                collaboratorRepository.findActiveByAirTransportCompanyId(selected.identity()));

        updateSelectedCollaborator(collaborators);
    }

    private void updateFromAirControlArea() {
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
        final List<CustomerCollaborator> collaborators = toList(
                collaboratorRepository.findActiveByAirControlAreaId(selected.identity()));

        updateSelectedCollaborator(collaborators);
    }

    private void updateSelectedCollaborator(final List<CustomerCollaborator> collaborators) {
        if (collaborators.isEmpty()) {
            System.out.println("No active collaborators found.");
            return;
        }

        for (int i = 0; i < collaborators.size(); i++) {
            final CustomerCollaborator c = collaborators.get(i);
            final String email = userRepository.ofIdentity(c.userId())
                    .map(user -> user.email())
                    .orElse(c.userId().toString());
            System.out.printf("%d - %s | %s%n", i + 1, email, c.position());
        }

        final int choice = Console.readInteger("Choose collaborator to update: ");
        if (choice < 1 || choice > collaborators.size()) {
            System.out.println("Invalid option.");
            return;
        }

        final CustomerCollaborator selected = collaborators.get(choice - 1);

        System.out.println("\nLeave empty to keep the current value:");
        final String newEmail = Console.readLine("New Email (optional)");
        final String newPhoneNumber = Console.readLine("New Phone Number (optional)");

        if (newEmail.isBlank() && newPhoneNumber.isBlank()) {
            System.out.println("At least one field must be provided for update.");
            return;
        }

        try {
            final User updatedUser = controller.updateCollaboratorContactInfo(
                    selected.userId().toString(),
                    newEmail.isBlank() ? null : newEmail,
                    newPhoneNumber.isBlank() ? null : newPhoneNumber
            );

            System.out.println("\nCustomer collaborator information successfully updated.");
            System.out.println("Email: " + updatedUser.email());
            System.out.println("Phone: " + updatedUser.phoneNumber());
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        for (final T item : iterable) {
            list.add(item);
        }
        return list;
    }
}
