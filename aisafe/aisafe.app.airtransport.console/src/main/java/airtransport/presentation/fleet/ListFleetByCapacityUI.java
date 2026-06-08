package airtransport.presentation.fleet;

import aircraft.domain.Aircraft;
import airtransportcompany.domain.AirTransportCompany;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;
import shared.domain.UserId;

import java.util.List;
import java.util.stream.Collectors;

public class ListFleetByCapacityUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final var session = AuthzRegistry.authorizationService().session().orElseThrow(IllegalStateException::new);
            final var userId = UserId.valueOf(session.authenticatedUser().identity().toString());
            final CustomerCollaborator collaborator = PersistenceContext.repositories().customerCollaborators()
                    .findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Current user is not a customer collaborator"));

            if (collaborator.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
                System.out.println("You are not assigned to an air transport company.");
                return true;
            }

            final AirTransportCompany company = PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(collaborator.airTransportCompanyId())
                    .orElseThrow(() -> new IllegalStateException("Air transport company not found"));

            final String input = Console.readLine("Enter minimum total passenger capacity");
            if (input == null || input.isBlank()) {
                System.out.println("Capacity cannot be empty.");
                return true;
            }

            final int minCapacity;
            try {
                minCapacity = Integer.parseInt(input.trim());
            } catch (final NumberFormatException e) {
                System.out.println("Invalid number: " + input.trim());
                return true;
            }

            if (minCapacity < 0) {
                System.out.println("Capacity cannot be negative.");
                return true;
            }

            final List<Aircraft> fleet = PersistenceContext.repositories().aircraft()
                    .findByAirTransportCompanyId(collaborator.airTransportCompanyId());

            final List<Aircraft> filtered = fleet.stream()
                    .filter(a -> {
                        final int total = a.cabinConfiguration().economyClassSeats()
                                + a.cabinConfiguration().businessClassSeats()
                                + a.cabinConfiguration().firstClassSeats();
                        return total >= minCapacity;
                    })
                    .collect(Collectors.toList());

            System.out.println("\n=== Fleet for: " + company.name() + " — Capacity >= " + minCapacity + " passengers ===\n");

            if (filtered.isEmpty()) {
                System.out.println("No aircraft found with capacity >= " + minCapacity + ".");
                return true;
            }

            System.out.printf("%-12s | %-25s | %-10s | %-10s | %-10s | %-12s | %-12s%n",
                    "REGISTRATION", "MODEL", "ECONOMY", "BUSINESS", "FIRST", "TOTAL", "STATUS");
            System.out.println("-".repeat(110));

            for (final Aircraft aircraft : filtered) {
                final int economy = aircraft.cabinConfiguration().economyClassSeats();
                final int business = aircraft.cabinConfiguration().businessClassSeats();
                final int first = aircraft.cabinConfiguration().firstClassSeats();
                final int total = economy + business + first;
                System.out.printf("%-12s | %-25s | %-10d | %-10d | %-10d | %-12d | %-12s%n",
                        aircraft.registrationNumber(),
                        aircraft.aircraftModel().modelName(),
                        economy,
                        business,
                        first,
                        total,
                        aircraft.operationStatus().toString());
            }

            System.out.println("-".repeat(110));
            System.out.printf("Total Aircraft: %d%n", filtered.size());
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() {
        return "List Fleet By Capacity";
    }
}
