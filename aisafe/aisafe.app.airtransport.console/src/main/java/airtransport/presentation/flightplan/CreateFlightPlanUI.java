package airtransport.presentation.flightplan;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import flight.dsl.ast.FlightPlanAst;
import flight.dsl.ast.LegAst;
import flight.dsl.ast.SegmentAst;
import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;
import flightPlan.application.ImportFlightPlanController;
import flightPlan.application.LegImportRequest;
import flightPlan.application.SegmentImportRequest;
import flightPlan.domain.FlightPlan;
import infrastructure.persistence.PersistenceContext;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CreateFlightPlanUI extends AbstractUI {

    private final ImportFlightPlanController importCtrl = new ImportFlightPlanController(
            PersistenceContext.repositories().flightPlans(),
            PersistenceContext.repositories().nodes(),
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().airports());

    private final FlightDslProcessor dslProcessor = new FlightDslProcessor();

    @Override
    protected boolean doShow() {
        System.out.println();
        System.out.println("  1. Create interactively");
        System.out.println("  2. Import from DSL file");
        final String choice = Console.readLine("  Select option");

        if ("2".equals(choice != null ? choice.trim() : "")) {
            return doImportFromFile();
        }
        System.out.println("  Interactive creation is not yet implemented.");
        return false;
    }

    // ── DSL file import ────────────────────────────────────────────────────────

    private boolean doImportFromFile() {
        final String path = promptNonEmpty("Path to DSL file");
        try {
            final FlightDslProcessingResult result = dslProcessor.process(Paths.get(path));
            if (!result.isValid()) {
                System.out.println("  DSL errors:");
                result.semanticErrors().forEach(e -> System.out.println("    - " + e));
                return false;
            }
            int imported = 0;
            for (final FlightPlanAst planAst : result.document().flightPlans()) {
                try {
                    final FlightPlan plan = importCtrl.importFlightPlan(
                            planAst.aircraftId(),
                            planAst.pilotRef(),
                            planAst.routeReference(),
                            buildLegRequests(planAst));
                    System.out.println("  Imported plan: " + plan.identity());
                    imported++;
                } catch (final Exception e) {
                    System.out.println("  ERROR importing " + planAst.flightDesignator() + ": " + e.getMessage());
                }
            }
            if (imported == 0) System.out.println("  No flight plans were imported.");
        } catch (final Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
        return true;
    }

    private List<LegImportRequest> buildLegRequests(final FlightPlanAst planAst) {
        final List<LegImportRequest> legs = new ArrayList<>();
        int seq = 1;
        for (final LegAst leg : planAst.legs()) {
            final List<SegmentImportRequest> segs = new ArrayList<>();
            for (final SegmentAst seg : leg.segments()) {
                segs.add(new SegmentImportRequest(
                        seg.from().latitude(), seg.from().longitude(),
                        seg.to().latitude(),   seg.to().longitude()));
            }
            legs.add(new LegImportRequest(seq++,
                    leg.departure().date(), leg.departure().time(),
                    leg.arrival().date(),   leg.arrival().time(),
                    leg.fuel().quantity(),
                    leg.departure().airportCode(), leg.arrival().airportCode(),
                    segs));
        }
        return legs;
    }

    // ── Prompt helpers ─────────────────────────────────────────────────────────

    private String promptNonEmpty(final String label) {
        while (true) {
            final String raw = Console.readLine("  " + label);
            if (raw != null && !raw.trim().isEmpty()) return raw.trim();
            System.out.println("  ERROR: This field is required.");
        }
    }

    @Override
    public String headline() {
        return "Create Flight Plan";
    }
}
