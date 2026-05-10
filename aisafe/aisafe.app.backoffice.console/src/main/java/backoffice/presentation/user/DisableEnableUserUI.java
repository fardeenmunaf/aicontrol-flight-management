package backoffice.presentation.user;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.SelectWidget;

import user.application.DisableEnableUserController;
import user.domain.User;
import infrastructure.persistence.PersistenceContext;

public class DisableEnableUserUI extends AbstractUI {

    private final DisableEnableUserController controller =
            new DisableEnableUserController(PersistenceContext.repositories().users());

    @Override
    protected boolean doShow() {
        final Iterable<User> users = controller.allUsers();

        final SelectWidget<User> selector = new SelectWidget<>("Select a User to disable/enable:", users,
                user -> String.format("%s (%s) - Status: %s", user.email(), user.name(), user.status()));
        selector.show();
        final User selected = selector.selectedElement();

        if (selected == null) {
            return false;
        }

        final String action = selected.isActive() ? "disable" : "enable";
        final String confirm = Console.readLine("Are you sure you want to " + action + " this user? (Y/N)");
        if ("Y".equalsIgnoreCase(confirm)) {
            try {
                if (selected.isActive()) {
                    controller.disableUser(selected.email());
                    System.out.println("\nUser disabled successfully.");
                } else {
                    controller.enableUser(selected.email());
                    System.out.println("\nUser enabled successfully.");
                }
            } catch (Exception e) {
                System.out.println("\nError updating user: " + e.getMessage());
            }
        } else {
            System.out.println("\nOperation cancelled.");
        }

        return true;
    }

    @Override
    public String headline() {
        return "Disable/Enable User";
    }
}

