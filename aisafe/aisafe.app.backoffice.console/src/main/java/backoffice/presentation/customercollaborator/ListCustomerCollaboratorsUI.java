package backoffice.presentation.customercollaborator;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.application.ListCustomerCollaboratorsController;
import customercollaborator.domain.CustomerCollaborator;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;

public class ListCustomerCollaboratorsUI extends AbstractUI {

    private final AirTransportCompanyRepository airTransportCompanyRepository =
            PersistenceContext.repositories().airTransportCompanies();
    private final AirControlAreaRepository airControlAreaRepository =
            PersistenceContext.repositories().airControlAreas();

    private final ListCustomerCollaboratorsController controller =
            new ListCustomerCollaboratorsController(
                    PersistenceContext.repositories().customerCollaborators());

    @Override
    protected boolean doShow() {
        final int type = Console.readInteger("Customer type (1-Air Transport Company, 2-Air Control Area): ");

        if (type == 1) {
            listFromAirTransportCompany();
        } else if (type == 2) {
            listFromAirControlArea();
        } else {
            System.out.println("Invalid option.");
        }

        return false;
    }

    @Override
    public String headline() {
        return "List Customer Collaborators";
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

        try {
            final List<CustomerCollaborator> collaborators = toList(
                    controller.listActiveByAirTransportCompany(selected.identity().toString()));
            printCollaborators(collaborators);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
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

        try {
            final List<CustomerCollaborator> collaborators = toList(
                    controller.listActiveByAirControlArea(selected.identity().toString()));
            printCollaborators(collaborators);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void printCollaborators(final List<CustomerCollaborator> collaborators) {
        if (collaborators.isEmpty()) {
            System.out.println("No active collaborators found.");
            return;
        }

        for (final CustomerCollaborator c : collaborators) {
            System.out.printf("- %s | %s%n", c.userId(), c.position());
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
