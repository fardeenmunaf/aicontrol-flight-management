package backoffice.presentation.user;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import user.application.RegisterUserController;
import user.domain.Role;
import infrastructure.persistence.PersistenceContext;

public class RegisterUserUI extends AbstractUI {

    private final RegisterUserController controller =
            new RegisterUserController(PersistenceContext.repositories().users());

    @Override
    protected boolean doShow() {
        try {
            final String name = Console.readLine("User full name");
            final String email = Console.readLine("Email address");
            final String phoneNumber = Console.readLine("Phone number");
            final Role role = readRole();
            final String password = Console.readLine("Password");

            controller.registerUser(email, password, name, phoneNumber, role);

            System.out.println("\nUser registered successfully.");
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("\nError: " + ex.getMessage());
            return false;
        } catch (final Exception ex) {
            System.out.println("\nError registering user: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Register New User";
    }

    private Role readRole() {
        final Role[] values = Role.values();
        System.out.println("\nAvailable roles:");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d - %s%n", i + 1, values[i].designation());
        }
        final int choice = readInt("Select user role");
        if (choice < 1 || choice > values.length) {
            throw new IllegalArgumentException("Invalid role option");
        }
        return values[choice - 1];
    }

    private int readInt(final String prompt) {
        final String raw = Console.readLine(prompt);
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number: " + raw);
        }
    }
}

