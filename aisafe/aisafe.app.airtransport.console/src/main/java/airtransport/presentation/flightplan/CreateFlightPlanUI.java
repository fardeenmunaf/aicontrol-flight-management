package airtransport.presentation.flightplan;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import flight.dsl.mapping.CreationResult;
import flight.dsl.mapping.FlightPlanCreationService;
import infrastructure.persistence.PersistenceContext;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import aircraft.domain.Aircraft;
import flightRoute.domain.FlightRoute;
import pilot.domain.Pilot;
import pilot.domain.PilotCompanyAssignment;

public class CreateFlightPlanUI extends AbstractUI {

    private final FlightPlanCreationService creationService = new FlightPlanCreationService(
            PersistenceContext.repositories().flightPlans(),
            PersistenceContext.repositories().airports(),
            PersistenceContext.repositories().nodes(),
            PersistenceContext.repositories().aircraft());

    @Override
    protected boolean doShow() {
        System.out.println();
        System.out.println("  1. Import from a DSL file");
        System.out.println("  2. Create interactively (build a DSL)");
        final String choice = Console.readLine("  Select option");
        final String trimmed = choice == null ? "" : choice.trim();
        if ("1".equals(trimmed)) {
            importFromFile();
        } else if ("2".equals(trimmed)) {
            createInteractively();
        } else {
            System.out.println("  Cancelled.");
        }
        return false;
    }

    private void importFromFile() {
        final String path = promptNonEmpty("Path to DSL file");
        try {
            report(creationService.createFromFile(Paths.get(path)));
        } catch (final Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    private void createInteractively() {
        System.out.println("  Profile/segment detail");
        System.out.println("  1. template");
        System.out.println("  2. prompt everything");
        final String detail = Console.readLine("  Select option [1]");
        final boolean promptEverything = "2".equals(detail == null ? "" : detail.trim());
        
        final String email = loggedInEmail();
        
        List<Aircraft> eligibleAircraft;
        List<FlightRoute> companyRoutes;
        String companyIcaoCode;
        try {
            eligibleAircraft = findEligibleAircraft(email);
            companyRoutes = findCompanyRoutes(email);
            companyIcaoCode = findCompanyIcaoCode(email);
        } catch (IllegalStateException e) {
            System.out.println("  Cannot create flight plan: " + e.getMessage());
            return;
        }

        if (eligibleAircraft.isEmpty()) {
            System.out.println("  Cannot create flight plan: No eligible aircraft found in company fleet.");
            return;
        }

        if (companyRoutes.isEmpty()) {
            System.out.println("  Cannot create flight plan: No routes found for the company.");
            return;
        }

        try {
            final String dsl = new ConsoleFlightDslBuilder(email, promptEverything, eligibleAircraft, companyRoutes, companyIcaoCode).build();
            report(creationService.createFromText(dsl));
        } catch (final IllegalArgumentException e) {
            System.out.println("  Validation error: " + e.getMessage());
        }
    }

    private List<FlightRoute> findCompanyRoutes(final String pilotEmail) {
        final Pilot pilot = PersistenceContext.repositories().pilots()
                .findByUserId(shared.domain.UserId.valueOf(pilotEmail))
                .orElseThrow(() -> new IllegalStateException("Pilot not found"));

        final PilotCompanyAssignment assignment = PersistenceContext.repositories().pilotCompanyAssignments()
                .findByPilotUserId(pilot.userId())
                .orElseThrow(() -> new IllegalStateException("Pilot is not assigned to a company"));

        Iterable<FlightRoute> routes = PersistenceContext.repositories().flightRoutes()
                .findByCompanyId(assignment.companyId());
        
        List<FlightRoute> list = new java.util.ArrayList<>();
        routes.forEach(list::add);
        return list;
    }

    private String findCompanyIcaoCode(final String pilotEmail) {
        final Pilot pilot = PersistenceContext.repositories().pilots()
                .findByUserId(shared.domain.UserId.valueOf(pilotEmail))
                .orElseThrow(() -> new IllegalStateException("Pilot not found"));

        final PilotCompanyAssignment assignment = PersistenceContext.repositories().pilotCompanyAssignments()
                .findByPilotUserId(pilot.userId())
                .orElseThrow(() -> new IllegalStateException("Pilot is not assigned to a company"));

        final airtransportcompany.domain.AirTransportCompany company = PersistenceContext.repositories().airTransportCompanies()
                .ofIdentity(assignment.companyId())
                .orElseThrow(() -> new IllegalStateException("Company not found"));
        
        return company.icaoCode();
    }

    private List<Aircraft> findEligibleAircraft(final String pilotEmail) {
        final Pilot pilot = PersistenceContext.repositories().pilots()
                .findByUserId(shared.domain.UserId.valueOf(pilotEmail))
                .orElseThrow(() -> new IllegalStateException("Pilot not found"));

        final PilotCompanyAssignment assignment = PersistenceContext.repositories().pilotCompanyAssignments()
                .findByPilotUserId(pilot.userId())
                .orElseThrow(() -> new IllegalStateException("Pilot is not assigned to a company"));

        final List<Aircraft> companyFleet = PersistenceContext.repositories().aircraft()
                .findByAirTransportCompanyId(assignment.companyId());

        final Set<aircraftmodel.domain.AircraftModel> certifiedModels = pilot.certifiedAircraftModels();

        return companyFleet.stream()
                .filter(a -> certifiedModels.contains(a.aircraftModel()))
                .collect(Collectors.toList());
    }

    private void report(final CreationResult result) {
        if (result.hasErrors()) {
            System.out.println("  Errors:");
            result.errors().forEach(e -> System.out.println("    - " + e));
        }
        if (!result.created().isEmpty()) {
            System.out.println("  Created " + result.created().size() + " flight plan(s):");
            result.created().forEach(p -> System.out.println(
                    "    ✓ " + p.flightDesignator() + " (" + p.identity() + ")"));
        } else if (!result.hasErrors()) {
            System.out.println("  No flight plans were created.");
        }
    }

    private static String promptNonEmpty(final String label) {
        while (true) {
            final String raw = Console.readLine("  " + label);
            if (raw != null && !raw.trim().isEmpty()) {
                return raw.trim();
            }
            System.out.println("  This field is required.");
        }
    }

    private static String loggedInEmail() {
        return AuthzRegistry.authorizationService().session()
                .map(s -> s.authenticatedUser().identity().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated session"));
    }

    @Override
    public String headline() {
        return "Create Flight Plan";
    }
}
