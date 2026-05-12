package backoffice.presentation.customercollaborator;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.io.util.Console;
import user.domain.User;
import user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class ListCustomerCollaboratorsUI {

    private final CustomerCollaboratorRepository collaboratorRepository;
    private final AirTransportCompanyRepository airTransportCompanyRepository;
    private final AirControlAreaRepository airControlAreaRepository;
    private final UserRepository userRepository;

    public ListCustomerCollaboratorsUI(final CustomerCollaboratorRepository collaboratorRepository,
                                       final AirTransportCompanyRepository airTransportCompanyRepository,
                                       final AirControlAreaRepository airControlAreaRepository,
                                       final UserRepository userRepository) {
        this.collaboratorRepository = collaboratorRepository;
        this.airTransportCompanyRepository = airTransportCompanyRepository;
        this.airControlAreaRepository = airControlAreaRepository;
        this.userRepository = userRepository;
    }

    public boolean show() {
        final int type = Console.readInteger("Customer type (1-Air Transport Company, 2-Air Control Area): ");

        if (type == 1) {
            listFromAirTransportCompany();
        } else if (type == 2) {
            listFromAirControlArea();
        } else {
            System.out.println("Invalid option.");
        }

        return true;
    }

    private void listFromAirTransportCompany() {
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
                collaboratorRepository.findActiveByAirTransportCompanyId(selected.identity())
        );

        printCollaborators(collaborators);
    }

    private void listFromAirControlArea() {
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
                collaboratorRepository.findActiveByAirControlAreaId(selected.identity())
        );

        printCollaborators(collaborators);
    }

    private void printCollaborators(final List<CustomerCollaborator> collaborators) {
        if (collaborators.isEmpty()) {
            System.out.println("No active collaborators found.");
            return;
        }

        for (final CustomerCollaborator c : collaborators) {
            final String userId = c.userId().toString();
            final String name = userRepository.findByEmail(userId)
                    .map(User::name)
                    .orElse(userId);

            System.out.printf("- %s | %s | %s%n",
                    name,
                    c.position(),
                    userId);
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