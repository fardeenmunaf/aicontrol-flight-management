package airtransport.presentation.flightroute;

import airtransportcompany.domain.AirTransportCompany;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.io.util.Console;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import flightRoute.application.DeleteFlightRouteController;
import flightRoute.domain.FlightRoute;
import infrastructure.persistence.PersistenceContext;
import shared.domain.UserId;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class DeleteFlightRouteUI extends AbstractUI {

    private final DeleteFlightRouteController controller = new DeleteFlightRouteController(
            PersistenceContext.repositories().flightRoutes(),
            PersistenceContext.repositories().flightPlans(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().airTransportCompanies());

    @Override
    protected boolean doShow() {
        try {
            final var session = AuthzRegistry.authorizationService().session()
                    .orElseThrow(IllegalStateException::new);
            final UserId userId = UserId.valueOf(session.authenticatedUser().identity().toString());

            final CustomerCollaborator collaborator = PersistenceContext.repositories()
                    .customerCollaborators().findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Current user is not a customer collaborator"));

            if (collaborator.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
                System.out.println("You are not assigned to an air transport company.");
                return true;
            }

            final AirTransportCompany company = PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(collaborator.airTransportCompanyId())
                    .orElseThrow(() -> new IllegalStateException("Air transport company not found"));

            final List<FlightRoute> routes = new ArrayList<>();
            PersistenceContext.repositories().flightRoutes()
                    .findByCompanyId(company.identity())
                    .forEach(r -> { if (r.isActive()) routes.add(r); });

            if (routes.isEmpty()) {
                System.out.println("No active flight routes found for your company.");
                return true;
            }

            System.out.printf("Active flight routes for %s:%n", company.name());
            for (int i = 0; i < routes.size(); i++) {
                final FlightRoute r = routes.get(i);
                System.out.printf("%d) %s  [%s -> %s]  Active from: %s%n",
                        i + 1,
                        r.designation(),
                        r.originAirport().iataCode(),
                        r.destinationAirport().iataCode(),
                        r.activeFrom());
            }

            final String choiceStr = Console.readLine("Select route number to deactivate");
            int choice;
            try {
                choice = Integer.parseInt(choiceStr.trim());
            } catch (final NumberFormatException e) {
                System.out.println("Invalid selection.");
                return true;
            }

            if (choice < 1 || choice > routes.size()) {
                System.out.println("Invalid selection.");
                return true;
            }

            final FlightRoute selected = routes.get(choice - 1);

            final String dateStr = Console.readLine("Deactivation date (YYYY-MM-DD)");
            LocalDate deactivationDate;
            try {
                deactivationDate = LocalDate.parse(dateStr.trim());
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date format. Use YYYY-MM-DD.");
                return true;
            }

            final String confirm = Console.readLine(
                    "Deactivate route " + selected.designation() + " from " + deactivationDate + "? (yes/no)");
            if (confirm == null || !confirm.trim().equalsIgnoreCase("yes")) {
                System.out.println("Cancelled.");
                return true;
            }

            controller.deleteFlightRoute(selected.identity().toString(), deactivationDate);
            System.out.printf("Route %s deactivated successfully. Last active day: %s%n",
                    selected.designation(), deactivationDate);

        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() {
        return "Deactivate Flight Route (US074)";
    }
}
