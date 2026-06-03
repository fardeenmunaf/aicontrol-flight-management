package airtransport.presentation.flightroute;

import airport.domain.Airport;
import airtransportcompany.domain.AirTransportCompany;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import flightRoute.application.CreateFlightRouteController;
import flightRoute.domain.FlightRoute;
import infrastructure.persistence.PersistenceContext;
import shared.domain.UserId;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CreateFlightRouteUI extends AbstractUI {

    private final CreateFlightRouteController controller = new CreateFlightRouteController(
            PersistenceContext.repositories().flightRoutes(),
            PersistenceContext.repositories().airports(),
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
                    .orElseThrow(() -> new IllegalStateException("Not a customer collaborator"));

            if (collaborator.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
                System.out.println("You are not assigned to an air transport company.");
                return true;
            }

            final AirTransportCompany company = PersistenceContext.repositories()
                    .airTransportCompanies().ofIdentity(collaborator.airTransportCompanyId())
                    .orElseThrow(() -> new IllegalStateException("Company not found"));

            final List<Airport> airports = new ArrayList<>();
            PersistenceContext.repositories().airports().findAll().forEach(airports::add);
            airports.sort((a, b) -> a.iataCode().compareTo(b.iataCode()));

            if (airports.isEmpty()) {
                System.out.println("No airports registered in the system. Cannot create a route.");
                return true;
            }

            System.out.printf("Company: %s (IATA: %s)%n", company.name(), company.iataCode());
            System.out.println();

            System.out.println("Available airports:");
            System.out.printf("  %-4s %-6s %-25s %-15s %s%n", "No.", "IATA", "Name", "Town", "Country");
            System.out.println("  " + "-".repeat(72));
            for (int i = 0; i < airports.size(); i++) {
                final Airport a = airports.get(i);
                System.out.printf("  %-4d %-6s %-25s %-15s %s%n",
                        i + 1, a.iataCode(), a.name(), a.town(), a.country());
            }
            System.out.println();

            final Airport origin = selectAirport(airports, "Select origin airport number");
            if (origin == null) return true;

            final Airport destination = selectAirport(airports, "Select destination airport number");
            if (destination == null) return true;

            System.out.printf("%nRoute designation starts with your company IATA code: %s%n", company.iataCode());
            final String suffix = Console.readLine("Enter the numeric suffix (1-4 digits, e.g. 123)");
            final String designation = company.iataCode() + suffix.trim();

            final String activeDateStr = Console.readLine("Active from date (YYYY-MM-DD, leave blank for today)");
            LocalDate activeFrom;
            if (activeDateStr == null || activeDateStr.trim().isEmpty()) {
                activeFrom = LocalDate.now();
            } else {
                try {
                    activeFrom = LocalDate.parse(activeDateStr.trim());
                } catch (final DateTimeParseException e) {
                    System.out.println("Invalid date format. Use YYYY-MM-DD.");
                    return true;
                }
            }

            final FlightRoute route = controller.createFlightRoute(
                    designation, origin.iataCode(), destination.iataCode(), activeFrom);

            System.out.println();
            System.out.println("Flight route created successfully:");
            System.out.printf("  Designation : %s%n", route.designation());
            System.out.printf("  Origin      : %s (%s)%n", route.originAirport().name(), route.originAirport().iataCode());
            System.out.printf("  Destination : %s (%s)%n", route.destinationAirport().name(), route.destinationAirport().iataCode());
            System.out.printf("  Active from : %s%n", route.activeFrom());

        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    private Airport selectAirport(final List<Airport> airports, final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null) return null;
        try {
            final int choice = Integer.parseInt(raw.trim());
            if (choice < 1 || choice > airports.size()) {
                System.out.println("Invalid selection.");
                return null;
            }
            return airports.get(choice - 1);
        } catch (final NumberFormatException e) {
            System.out.println("Invalid number.");
            return null;
        }
    }

    @Override
    public String headline() {
        return "Create Flight Route (US073)";
    }
}
