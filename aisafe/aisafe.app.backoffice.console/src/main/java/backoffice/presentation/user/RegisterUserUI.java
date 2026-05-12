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
        System.out.println("1 - Register new user");
        System.out.println("2 - Edit existing user (roles/phone)");
        int option = readInt("Select option");
        switch (option) {
            case 1:
                return doRegisterUser();
            case 2:
                return doEditUser();
            default:
                System.out.println("Invalid option");
                return false;
        }
    }

    private boolean doRegisterUser() {
        try {
            final String name = Console.readLine("User full name");
            final String email = Console.readLine("Email address");
            final String phoneNumber = Console.readLine("Phone number");
            final String password = Console.readLine("Password");
            final java.util.Set<Role> roles = readRoles();

            controller.registerUser(email, password, name, phoneNumber, roles);

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

    private boolean doEditUser() {
        try {
            final String email = Console.readLine("Email address of user to edit");
            final String newPhone = Console.readLine("New phone number");
            final java.util.Set<Role> newRoles = readRoles();
            controller.editUser(email, newPhone, newRoles);
            System.out.println("\nUser updated successfully.");
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("\nError: " + ex.getMessage());
            return false;
        } catch (final Exception ex) {
            System.out.println("\nError updating user: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Register New User";
    }

    private java.util.Set<Role> readRoles() {
        final Role[] values = Role.values();
        System.out.println("\nAvailable roles:");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d - %s%n", i + 1, values[i].designation());
        }
        System.out.println("Enter role numbers separated by commas (e.g., 1,3):");
        final String input = Console.readLine("Select user roles");
        String[] parts = input.split(",");
        java.util.Set<Role> selected = new java.util.HashSet<>();
        for (String part : parts) {
            try {
                int idx = Integer.parseInt(part.trim());
                if (idx < 1 || idx > values.length) {
                    throw new IllegalArgumentException("Invalid role option: " + idx);
                }
                selected.add(values[idx - 1]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number: " + part);
            }
        }
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be selected");
        }
        return selected;
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

