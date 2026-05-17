package airtransport.presentation;

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

public class ListFleetByMakerUI extends AbstractUI {

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

            final String makerFilter = Console.readLine("Enter maker name to search");
            if (makerFilter == null || makerFilter.isBlank()) {
                System.out.println("Maker name cannot be empty.");
                return true;
            }

            final List<Aircraft> fleet = PersistenceContext.repositories().aircraft()
                    .findByAirTransportCompanyId(collaborator.airTransportCompanyId());

            final String lower = makerFilter.trim().toLowerCase();
            final List<Aircraft> filtered = fleet.stream()
                    .filter(a -> a.aircraftModel().maker().name().toLowerCase().contains(lower))
                    .collect(Collectors.toList());

            System.out.println("\n=== Fleet for: " + company.name() + " — filtered by Maker: \"" + makerFilter.trim() + "\" ===\n");

            if (filtered.isEmpty()) {
                System.out.println("No aircraft found with maker \"" + makerFilter.trim() + "\".");
                return true;
            }

            System.out.printf("%-12s | %-25s | %-15s | %-12s | %-10s%n",
                    "REGISTRATION", "MODEL", "MAKER", "STATUS", "COUNTRY");
            System.out.println("-".repeat(90));

            for (final Aircraft aircraft : filtered) {
                System.out.printf("%-12s | %-25s | %-15s | %-12s | %-10s%n",
                        aircraft.registrationNumber(),
                        aircraft.aircraftModel().modelName(),
                        aircraft.aircraftModel().maker().name(),
                        aircraft.operationStatus().toString(),
                        aircraft.registrationCountry());
            }

            System.out.println("-".repeat(90));
            System.out.printf("Total Aircraft: %d%n", filtered.size());
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() {
        return "List Fleet By Maker";
    }
}
