package airtransport.presentation.flightplan;

import auth.domain.AISafeRoles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import flight.dsl.ast.ClimbEntryAst;
import flight.dsl.ast.DescendEntryAst;
import flight.dsl.ast.FlightPlanAst;
import flight.dsl.ast.LegAst;
import flight.dsl.ast.SegmentAst;
import flight.dsl.model.FlightDslDocument;
import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;
import flight.dsl.validation.SemanticError;
import flightPlan.application.ImportFlightPlanController;
import flightPlan.application.LegImportRequest;
import flightPlan.application.SegmentImportRequest;
import flightPlan.domain.FlightPlan;
import infrastructure.persistence.PersistenceContext;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guides the pilot through every field defined in the FlightDSL grammar,
 * builds a DSL document from the answers, validates it (lexical → syntactic →
 * semantic)
 * and imports the result if valid.
 */
public class CreateFlightPlanUI extends AbstractUI {

    private final ImportFlightPlanController controller = new ImportFlightPlanController(
            PersistenceContext.repositories().flightPlans(),
            PersistenceContext.repositories().nodes(),
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().airports(),
            PersistenceContext.repositories().airTransportCompanies());

    private final FlightDslProcessor processor = new FlightDslProcessor();

    @Override
    protected boolean doShow() {
        if (!AuthzRegistry.authorizationService().isAuthenticatedUserAuthorizedTo(AISafeRoles.PILOT)) {
            System.out.println("Access denied. Only pilots may create flight plans.");
            return true;
        }

        System.out.println("How do you want to create the flight plan?");
        System.out.println("  1 - Fill in fields interactively");
        System.out.println("  2 - Import from an existing .dsl file");
        final String choice = Console.readLine("Option [1]");

        if ("2".equals(choice != null ? choice.trim() : "")) {
            importFromFile();
        } else {
            final String dsl = buildDsl();
            System.out.println();
            System.out.println("+=== Generated Flight DSL =======================================+");
            System.out.println(dsl);
            System.out.println("+================================================================+");
            System.out.println();
            validate(dsl);
        }
        return true;
    }

    private void importFromFile() {
        final String raw = Console.readLine("Path to .dsl file");
        if (raw == null || raw.trim().isEmpty()) {
            System.out.println("No path provided.");
            return;
        }
        final java.nio.file.Path path = Paths.get(raw.trim());
        if (!path.toFile().exists()) {
            System.out.println("File not found: " + path.toAbsolutePath());
            return;
        }
        if (!path.toFile().isFile() || !raw.trim().toLowerCase().endsWith(".dsl")) {
            System.out.println("File must be a .dsl file.");
            return;
        }

        System.out.println("Reading file: " + path.toAbsolutePath());
        System.out.println();

        try {
            final String dsl = java.nio.file.Files.readString(path);
            System.out.println("+=== Flight DSL File Contents ===================================+");
            System.out.println(dsl);
            System.out.println("+================================================================+");
            System.out.println();
            validate(dsl);
        } catch (final IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // DSL builder — mirrors the FlightDSL.g4 grammar structure
    // -------------------------------------------------------------------------

    private String buildDsl() {
        final StringBuilder sb = new StringBuilder();

        // flightPlan: FLIGHT ID LBRACE type route leg+ RBRACE
        final String pilotEmail = AuthzRegistry.authorizationService().session()
                .map(s -> s.authenticatedUser().identity().toString())
                .orElse("pilot@example.com");

        final String designator = prompt("Flight designator", "TP1234",
                "Two uppercase letters + 1-4 digits + optional letter suffix");
        final String type = promptChoice("Flight type", new String[] { "regular", "charter" }, "regular");
        final String aircraftId = prompt("Aircraft registration", "A380",
                "Must start with a letter, letters and digits only (e.g. A380, B777, CS300)");
        final String routeRef = prompt("Route reference identifier", "AA123", "Letter followed by letters/digits");
        final String origin = prompt("Route origin airport code (IATA or ICAO)", "OPO",
                "3-letter IATA or 4-letter ICAO, uppercase");
        final String dest = prompt("Route destination airport code (IATA or ICAO)", "MAD",
                "3-letter IATA or 4-letter ICAO, uppercase");

        sb.append("flight ").append(designator).append(" {\n");
        sb.append("    type:      ").append(type).append(";\n");
        sb.append("    aircraft:  ").append(aircraftId).append(";\n");
        sb.append("    pilot:     ").append(pilotEmail).append(";\n");
        sb.append("    route:     ").append(routeRef)
                .append(" from ").append(origin)
                .append(" to ").append(dest).append(";\n");

        // leg+
        int legNum = 1;
        do {
            System.out.println();
            System.out.println("+--- Leg " + legNum + " ---------------------------------------------------+");
            sb.append("\n    leg {\n");
            buildLeg(sb);
            sb.append("    }\n");
            legNum++;
        } while (confirm("Add another leg?", false));

        sb.append("}\n");
        return sb.toString();
    }

    /** leg: departure arrival fuel profile? climbSegment cruiseSegment descendSegment */
    private void buildLeg(final StringBuilder sb) {
        buildDeparture(sb);
        buildArrival(sb);
        buildFuel(sb);
        if (confirm("Add optional flight profile (climb/descend tables)?", false)) {
            buildProfile(sb);
        }

        System.out.println();
        System.out.println("  -- Segment 1: Climb --");
        buildSegment(sb, "climb");

        System.out.println();
        System.out.println("  -- Segment 2: Cruise --");
        buildSegment(sb, "cruise");

        System.out.println();
        System.out.println("  -- Segment 3: Descend --");
        buildSegment(sb, "descend");
    }

    /** departure: DEPARTURE LBRACE airport date time RBRACE */
    private void buildDeparture(final StringBuilder sb) {
        System.out.println();
        System.out.println("  Departure:");
        final String airport = prompt("    Airport code", "OPO", "e.g. OPO (IATA) or LPPR (ICAO)");
        final String date = prompt("    Date", "2026-05-05", "YYYY-MM-DD");
        final String time = prompt("    Time", "08:00", "HH:MM  (24-hour clock)");
        sb.append("        departure {\n")
                .append("            airport: ").append(airport).append(";\n")
                .append("            date:    ").append(date).append(";\n")
                .append("            time:    ").append(time).append(";\n")
                .append("        }\n");
    }

    /** arrival: ARRIVAL LBRACE airport date time RBRACE */
    private void buildArrival(final StringBuilder sb) {
        System.out.println();
        System.out.println("  Arrival:");
        final String airport = prompt("    Airport code", "MAD", "e.g. MAD (IATA) or LEMD (ICAO)");
        final String date = prompt("    Date", "2026-05-05", "YYYY-MM-DD");
        final String time = prompt("    Time", "09:30", "HH:MM  (24-hour clock)");
        sb.append("        arrival {\n")
                .append("            airport: ").append(airport).append(";\n")
                .append("            date:    ").append(date).append(";\n")
                .append("            time:    ").append(time).append(";\n")
                .append("        }\n");
    }

    /** fuel: FUEL LBRACE QUANTITY COLON positiveNumber unit SEMI RBRACE */
    private void buildFuel(final StringBuilder sb) {
        System.out.println();
        System.out.println("  Fuel:");
        final String qty = prompt("    Quantity (must be > 0)", "40200",
                "Positive integer or decimal, e.g. 40200  or  18500.5");
        final String unit = promptChoice("    Unit", new String[] { "kg", "l" }, "kg");
        sb.append("        fuel {\n")
                .append("            quantity: ").append(qty).append(" ").append(unit).append(";\n")
                .append("        }\n");
    }

    /**
     * profile: PROFILE LBRACE profileEntry+ RBRACE
     * profileEntry: climbEntry | descendEntry
     * climbEntry: CLIMB [ altitude: num unit, speed: num unit ];
     * descendEntry: DESCEND [ altitude: num unit, speed: num unit, rate:
     * signedNumber ];
     */
    private void buildProfile(final StringBuilder sb) {
        System.out.println();
        System.out.println("  Flight Profile:");
        sb.append("        profile {\n");
        do {
            final String kind = promptChoice("    Entry type", new String[] { "climb", "descend" }, "climb");
            final String alt = prompt("      Altitude value (>= 0)", "1000", "e.g. 0  or  10000");
            final String altUnit = promptChoice("      Altitude unit", new String[] { "m", "ft" }, "m");
            final String spd = prompt("      Speed value (> 0)", "210", "e.g. 210  or  300");
            final String spdUnit = promptChoice("      Speed unit", new String[] { "kts", "m/s" }, "kts");

            if ("climb".equals(kind)) {
                sb.append("            climb   [ altitude: ").append(alt).append(" ").append(altUnit)
                        .append(", speed: ").append(spd).append(" ").append(spdUnit).append(" ];\n");
            } else {
                final String rate = prompt("      Rate of descent (must be negative)", "-10",
                        "Negative integer or decimal, e.g. -10  or  -7.5");
                sb.append("            descend [ altitude: ").append(alt).append(" ").append(altUnit)
                        .append(", speed: ").append(spd).append(" ").append(spdUnit)
                        .append(", rate: ").append(rate).append(" ];\n");
            }
        } while (confirm("    Add another profile entry?", false));
        sb.append("        }\n");
    }

    /**
     * segment: SEGMENT LBRACE from to altslot+ wind mode RBRACE
     */
    private void buildSegment(final StringBuilder sb, final String mode) {
        System.out.println("    From coordinate:");
        final String fromLat = prompt("      Latitude", "41.262891", "Signed decimal, e.g. 41.262891");
        final String fromLon = prompt("      Longitude", "-8.68522", "Signed decimal");
        final String fromAlt = prompt("      Altitude value (>= 0)", "69", "Positive number, e.g. 69");
        final String fromAltUnit = promptChoice("      Altitude unit", new String[] { "m", "ft" }, "m");

        System.out.println("    To coordinate (must differ from From):");
        final String toLat = prompt("      Latitude", "40.4895", "Signed decimal");
        final String toLon = prompt("      Longitude", "-3.5643", "Signed decimal");
        final String toAlt = prompt("      Altitude value (>= 0)", "610", "Positive number");
        final String toAltUnit = promptChoice("      Altitude unit", new String[] { "m", "ft" }, "m");

        sb.append("        segment {\n")
                .append("            from: (").append(fromLat).append(", ").append(fromLon)
                .append(", altitude: ").append(fromAlt).append(" ").append(fromAltUnit).append(");\n")
                .append("            to:   (").append(toLat).append(", ").append(toLon)
                .append(", altitude: ").append(toAlt).append(" ").append(toAltUnit).append(");\n");

        // Altitude slots (at least one required)
        int slotNum = 1;
        do {
            System.out.println("    Altitude slot " + slotNum + ":");
            final String minAlt = prompt("      Min altitude (>= 0)", "0", "e.g. 0");
            final String minUnit = promptChoice("      Min unit", new String[] { "m", "ft" }, "m");
            final String maxAlt = prompt("      Max altitude (> min)", "9249", "e.g. 9249");
            final String maxUnit = promptChoice("      Max unit", new String[] { "m", "ft" }, "m");
            final String width = prompt("      Width (> 0)", "50", "Corridor width, e.g. 50");
            final String widthUnit = promptChoice("      Width unit", new String[] { "m", "ft" }, "m");
            sb.append("            altslot [ min: ").append(minAlt).append(" ").append(minUnit)
                    .append(", max: ").append(maxAlt).append(" ").append(maxUnit)
                    .append(", width: ").append(width).append(" ").append(widthUnit).append(" ];\n");
            slotNum++;
        } while (confirm("    Add another altitude slot?", false));

        // Wind (mandatory)
        System.out.println("    Wind:");
        final String windDir = prompt("      Direction (0-359 degrees)", "270", "Angle relative to North, e.g. 270 = West");
        final String windSpd = prompt("      Speed (m/s, > 0)", "15", "e.g. 15");
        sb.append("            wind { direction: ").append(windDir)
                .append("; speed: ").append(windSpd).append(" m/s; }\n");

        sb.append("            mode: ").append(mode).append(";\n");

        sb.append("        }\n");
    }

    // -------------------------------------------------------------------------
    // Validation & import
    // -------------------------------------------------------------------------

    private void validate(final String dsl) {
        System.out.println("Validating (lexical → syntactic → semantic)...");
        System.out.println();

        FlightDslProcessingResult result;
        try {
            result = processor.process(dsl);
        } catch (final ParseCancellationException e) {
            System.out.println("VALIDATION FAILED — Lexical / Syntactic error:");
            System.out.println("  " + e.getMessage());
            return;
        }

        if (!result.isValid()) {
            System.out.println("VALIDATION FAILED — " + result.semanticErrors().size() + " semantic error(s):");
            for (final SemanticError err : result.semanticErrors()) {
                System.out.printf("  [%s] line %d, col %d: %s%n",
                        err.type(), err.line(), err.column(), err.message());
            }
            return;
        }

        System.out.println("VALID — no errors found.");
        System.out.printf("  Flights: %d   Legs: %d   Segments: %d%n",
                result.summary().flightPlanCount(),
                result.summary().legCount(),
                result.summary().segmentCount());
        System.out.println();

        exportToJson(result.document());

        if (!confirm("Import this flight plan into the system?", true)) {
            System.out.println("Import cancelled.");
            return;
        }

        int ok = 0;
        int fail = 0;
        for (final FlightPlanAst fp : result.document().flightPlans()) {
            try {
                final List<LegImportRequest> legReqs = buildLegRequests(fp.legs());
                final FlightPlan saved = controller.importFlightPlan(fp.flightDesignator(), fp.aircraftId(), fp.pilotRef(), legReqs);
                System.out.println("  Imported: " + saved.identity());
                ok++;
            } catch (final Exception e) {
                System.out.println("  FAILED (" + fp.flightDesignator() + "): " + e.getMessage());
                fail++;
            }
        }
        System.out.printf("%nDone. Imported: %d   Failed: %d%n", ok, fail);
    }

    private void exportToJson(final FlightDslDocument document) {
        final String firstName = document.flightPlans().isEmpty()
                ? "flight_plan"
                : document.flightPlans().get(0).flightDesignator();
        final Path outputDir = Paths.get("aisafe-simulation/flightplans");
        final String defaultPath = outputDir.resolve(firstName + ".json").toString();
        final String raw = Console.readLine("  Export JSON to file [" + defaultPath + "]");
        final String filePath = (raw == null || raw.trim().isEmpty()) ? defaultPath : raw.trim();

        final List<Map<String, Object>> plans = new ArrayList<>();
        for (final FlightPlanAst fp : document.flightPlans()) {
            final Map<String, Object> planMap = new LinkedHashMap<>();
            planMap.put("flightDesignator", fp.flightDesignator());
            planMap.put("type", fp.type().name().toLowerCase());
            planMap.put("aircraftId", fp.aircraftId());
            planMap.put("pilotRef", fp.pilotRef());

            final Map<String, String> routeMap = new LinkedHashMap<>();
            routeMap.put("reference", fp.routeReference());
            routeMap.put("origin", fp.routeOrigin());
            routeMap.put("destination", fp.routeDestination());
            planMap.put("route", routeMap);

            final List<Map<String, Object>> legList = new ArrayList<>();
            for (final LegAst leg : fp.legs()) {
                final Map<String, Object> legMap = new LinkedHashMap<>();

                final Map<String, String> dep = new LinkedHashMap<>();
                dep.put("airport", leg.departure().airportCode());
                dep.put("date", leg.departure().date());
                dep.put("time", leg.departure().time());
                legMap.put("departure", dep);

                final Map<String, String> arr = new LinkedHashMap<>();
                arr.put("airport", leg.arrival().airportCode());
                arr.put("date", leg.arrival().date());
                arr.put("time", leg.arrival().time());
                legMap.put("arrival", arr);

                final Map<String, Object> fuelMap = new LinkedHashMap<>();
                fuelMap.put("quantity", leg.fuel().quantity());
                fuelMap.put("unit", leg.fuel().unit());
                legMap.put("fuel", fuelMap);

                if (leg.profile() != null) {
                    final List<Map<String, Object>> profileList = new ArrayList<>();
                    for (final ClimbEntryAst c : leg.profile().climbEntries()) {
                        final Map<String, Object> e = new LinkedHashMap<>();
                        e.put("type", "climb");
                        e.put("altitude", c.altitude());
                        e.put("altitudeUnit", c.altitudeUnit());
                        e.put("speed", c.speed());
                        e.put("speedUnit", c.speedUnit());
                        profileList.add(e);
                    }
                    for (final DescendEntryAst d : leg.profile().descendEntries()) {
                        final Map<String, Object> e = new LinkedHashMap<>();
                        e.put("type", "descend");
                        e.put("altitude", d.altitude());
                        e.put("altitudeUnit", d.altitudeUnit());
                        e.put("speed", d.speed());
                        e.put("speedUnit", d.speedUnit());
                        e.put("rate", d.rate());
                        profileList.add(e);
                    }
                    legMap.put("profile", profileList);
                }

                final List<Map<String, Object>> segList = new ArrayList<>();
                for (final SegmentAst seg : leg.segments()) {
                    final Map<String, Object> segMap = new LinkedHashMap<>();
                    segMap.put("mode", seg.mode().name().toLowerCase());

                    final Map<String, Object> from = new LinkedHashMap<>();
                    from.put("latitude", seg.from().latitude());
                    from.put("longitude", seg.from().longitude());
                    from.put("altitude", seg.from().altitude());
                    from.put("altitudeUnit", seg.from().altitudeUnit());
                    segMap.put("from", from);

                    final Map<String, Object> to = new LinkedHashMap<>();
                    to.put("latitude", seg.to().latitude());
                    to.put("longitude", seg.to().longitude());
                    to.put("altitude", seg.to().altitude());
                    to.put("altitudeUnit", seg.to().altitudeUnit());
                    segMap.put("to", to);

                    segList.add(segMap);
                }
                legMap.put("segments", segList);
                legList.add(legMap);
            }
            planMap.put("legs", legList);
            plans.add(planMap);
        }

        try {
            final Path path = Paths.get(filePath);
            new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValue(path.toFile(), plans);
            System.out.println("JSON exported to: " + path.toAbsolutePath());
        } catch (final IOException e) {
            System.out.println("WARNING: could not write JSON file: " + e.getMessage());
        }
    }

    private List<LegImportRequest> buildLegRequests(final List<LegAst> legs) {
        final List<LegImportRequest> reqs = new ArrayList<>();
        for (int i = 0; i < legs.size(); i++) {
            final LegAst leg = legs.get(i);
            final List<SegmentImportRequest> segReqs = new ArrayList<>();
            for (final SegmentAst seg : leg.segments()) {
                segReqs.add(new SegmentImportRequest(
                        seg.from().latitude(), seg.from().longitude(),
                        seg.to().latitude(), seg.to().longitude()));
            }
            reqs.add(new LegImportRequest(
                    i + 1,
                    leg.departure().date(), leg.departure().time(),
                    leg.arrival().date(), leg.arrival().time(),
                    leg.fuel().quantity(),
                    leg.departure().airportCode(),
                    leg.arrival().airportCode(),
                    segReqs));
        }
        return reqs;
    }

    // -------------------------------------------------------------------------
    // Input helpers
    // -------------------------------------------------------------------------

    /**
     * Prompts the user and returns the trimmed input.
     * If the user presses ENTER with no input, the example value is used.
     */
    private String prompt(final String label, final String example, final String hint) {
        System.out.println("  Hint: " + hint);
        final String raw = Console.readLine("  " + label + " [" + example + "]");
        if (raw == null || raw.trim().isEmpty()) {
            return example;
        }
        return raw.trim();
    }

    /** Prompts for a value restricted to a fixed set of options. */
    private String promptChoice(final String label, final String[] options, final String example) {
        final String opts = String.join(" | ", options);
        final String raw = Console.readLine("  " + label + " (" + opts + ") [" + example + "]");
        if (raw == null || raw.trim().isEmpty()) {
            return example;
        }
        return raw.trim().toLowerCase();
    }

    /** yes/no confirmation; defaults to the specified value. */
    private boolean confirm(final String question, final boolean defaultYes) {
        final String defStr = defaultYes ? "yes" : "no";
        final String raw = Console.readLine("  " + question + " (yes/no) [" + defStr + "]");
        if (raw == null || raw.trim().isEmpty()) {
            return defaultYes;
        }
        return raw.trim().equalsIgnoreCase("yes");
    }

    @Override
    public String headline() {
        return "Create Flight Plan";
    }
}
