package backoffice.presentation.authz;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class LoginUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        boolean authenticated = false;
        while (!authenticated) {
            final String email = Console.readLine("Email");
            final String password = Console.readLine("Password");

            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                System.out.println("Email and password cannot be empty. Please try again.");
                continue;
            }

            try {
                authenticated = AuthzRegistry.authenticationService()
                        .authenticate(email, password)
                        .isPresent();
            } catch (IllegalArgumentException e) {
                authenticated = false;
            }

            if (!authenticated) {
                System.out.println("Invalid credentials. Please try again.");
            }
        }
        return true;
    }

    @Override
    public String headline() {
        return "Login";
    }
}
