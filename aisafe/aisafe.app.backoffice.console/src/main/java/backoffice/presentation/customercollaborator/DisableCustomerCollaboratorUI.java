package backoffice.presentation.customercollaborator;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import customercollaborator.application.DisableCustomerCollaboratorController;
import customercollaborator.application.ListCustomerCollaboratorsController;
import customercollaborator.domain.CustomerCollaborator;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;

public class DisableCustomerCollaboratorUI extends AbstractUI {

    private final DisableCustomerCollaboratorController controller =
            new DisableCustomerCollaboratorController(
                    PersistenceContext.repositories().customerCollaborators(),
                    PersistenceContext.repositories().users());

    private final ListCustomerCollaboratorsController listController =
            new ListCustomerCollaboratorsController(
                    PersistenceContext.repositories().customerCollaborators());

    private final AirTransportCompanyRepository airTransportCompanyRepository =
            PersistenceContext.repositories().airTransportCompanies();

    private final AirControlAreaRepository airControlAreaRepository =
            PersistenceContext.repositories().airControlAreas();

    @Override
    protected boolean doShow() {
        final int type = Console.readInteger("Customer type (1-Air Transport Company, 2-Air Control Area): ");

        if (type == 1) {
            disableFromAirTransportCompany();
        } else if (type == 2) {
            disableFromAirControlArea();
        } else {
            System.out.println("Invalid option.");
        }

        return false;
    }

    @Override
    public String headline() {
        return "Disable Customer Collaborator";
    }

    private void disableFromAirTransportCompany() {
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
                listController.listActiveByAirTransportCompany(selected.identity().toString()));

        disableSelectedCollaborator(collaborators);
    }

    private void disableFromAirControlArea() {
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
                listController.listActiveByAirControlArea(selected.identity().toString()));

        disableSelectedCollaborator(collaborators);
    }

    private void disableSelectedCollaborator(final List<CustomerCollaborator> collaborators) {
        if (collaborators.isEmpty()) {
            System.out.println("No active collaborators found.");
            return;
        }

        for (int i = 0; i < collaborators.size(); i++) {
            final CustomerCollaborator c = collaborators.get(i);
            System.out.printf("%d - %s | %s%n", i + 1, c.userId(), c.position());
        }

        final int choice = Console.readInteger("Choose collaborator to disable: ");
        if (choice < 1 || choice > collaborators.size()) {
            System.out.println("Invalid option.");
            return;
        }

        final CustomerCollaborator selected = collaborators.get(choice - 1);

        try {
            controller.disable(selected.userId().toString());
            System.out.println("Collaborator disabled.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
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
