package backoffice.presentation.customercollaborator;

import customercollaborator.application.RegisterCustomerCollaboratorController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

public class RegisterCustomerCollaboratorUI extends AbstractUI {

    private final RegisterCustomerCollaboratorController controller =
            new RegisterCustomerCollaboratorController(
                    PersistenceContext.repositories().users(),
                    PersistenceContext.repositories().customerCollaborators(),
                    PersistenceContext.repositories().airTransportCompanies(),
                    PersistenceContext.repositories().airControlAreas()
            );

    @Override
    protected boolean doShow() {
        try {
            final int option = Console.readInteger("Customer type (1-Air Transport Company, 2-Air Control Area)");
            final String customerId = Console.readLine("Customer id");
            final String email = Console.readLine("Email");
            final String rawPassword = Console.readLine("Password");
            final String name = Console.readLine("Name");
            final String phoneNumber = Console.readLine("Phone number");
            final String position = Console.readLine("Position");

            if (option == 1) {
                controller.registerForAirTransportCompany(customerId, email, rawPassword, name, phoneNumber, position);
            } else if (option == 2) {
                controller.registerForAirControlArea(customerId, email, rawPassword, name, phoneNumber, position);
            } else {
                throw new IllegalArgumentException("Invalid customer type");
            }

            System.out.println("Customer collaborator successfully registered.");
            return true;
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Register Customer Collaborator";
    }
}