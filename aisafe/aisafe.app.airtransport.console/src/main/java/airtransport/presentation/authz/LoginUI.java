package airtransport.presentation.authz;

import airtransport.AirTransportContext;
import airtransport.service.LocalAirTransportService;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import shared.domain.UserId;

import java.util.Optional;

public class LoginUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        boolean authenticated = false;
        while (!authenticated) {
            final String email    = Console.readLine("Email");
            final String password = Console.readLine("Password");

            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                System.out.println("Email and password cannot be empty. Please try again.");
                continue;
            }

            try {
                final Optional<UserSession> session =
                        AuthzRegistry.authenticationService().authenticate(email, password);
                if (session.isEmpty()) {
                    System.out.println("Invalid credentials. Please try again.");
                    continue;
                }

                final String userEmail = session.get().authenticatedUser().identity().toString();
                final CustomerCollaborator collab = PersistenceContext.repositories()
                        .customerCollaborators().findByUserId(UserId.valueOf(userEmail))
                        .orElse(null);

                if (collab == null || collab.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
                    System.out.println("Access denied: account is not linked to an air transport company.");
                    continue;
                }

                final String icaoCode = PersistenceContext.repositories().airTransportCompanies()
                        .ofIdentity(collab.airTransportCompanyId())
                        .map(c -> c.icaoCode())
                        .orElse("");

                AirTransportContext.setService(
                        new LocalAirTransportService(collab.airTransportCompanyId(), icaoCode));
                authenticated = true;
            } catch (final Exception e) {
                System.out.println("Login error: " + e.getMessage());
            }
        }
        return true;
    }

    @Override
    public String headline() {
        return "Login";
    }
}
