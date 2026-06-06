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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Guides the pilot through every field defined in the FlightDSL grammar,
 * validates each input immediately, builds a DSL document and imports it.
 */
public class CreateFlightPlanUI extends AbstractUI {

    // -------------------------------------------------------------------------
    // Validation patterns (mirror the grammar token rules)
    // -------------------------------------------------------------------------

    private static final Pattern DESIGNATOR = Pattern.compile("[A-Z]{2}[0-9]{1,4}[A-Z]?");
    private static final Pattern AIRCRAFT_ID = Pattern.compile("[A-Za-z][A-Za-z0-9]*");
    private static final Pattern ROUTE_REF = Pattern.compile("[A-Za-z][A-Za-z0-9]*");
    private static final Pattern AIRPORT = Pattern.compile("[A-Z]{3,4}");
    private static final Pattern DATE_FMT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TIME_FMT = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    // -------------------------------------------------------------------------

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
        final Path path = Paths.get(raw.trim());
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
            final String dsl = Files.readString(path);
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

        final String pilotEmail = AuthzRegistry.authorizationService().session()
                .map(s -> s.authenticatedUser().identity().toString())
                .orElse("pilot@example.com");

        final String designator = promptValidated(
                "Flight designator", "TP1234",
                "2 uppercase letters + 1-4 digits + optional uppercase letter (e.g. TP1234, BA100A, FR23)",
                v -> DESIGNATOR.matcher(v).matches(),
                "Must be exactly 2 uppercase letters followed by 1-4 digits and an optional uppercase letter.");

        final String type = promptChoice("Flight type", new String[] { "regular", "charter" }, "regular");

        final String aircraftId = promptValidated(
                "Aircraft registration", "A380",
                "Starts with a letter, then letters/digits only (e.g. A380, B777, CS300)",
                v -> AIRCRAFT_ID.matcher(v).matches(),
                "Must start with a letter; only letters and digits are allowed.");

        final String routeRef = promptValidated(
                "Route reference identifier", "AA123",
                "Letter followed by letters/digits (e.g. AA123, RT01)",
                v -> ROUTE_REF.matcher(v).matches(),
                "Must start with a letter; only letters and digits are allowed.");

        final String origin = promptValidated(
                "Route origin airport code (IATA or ICAO)", "OPO",
                "3-letter IATA (e.g. OPO) or 4-letter ICAO (e.g. LPPR), uppercase only",
                v -> AIRPORT.matcher(v).matches(),
                "Must be 3 uppercase letters (IATA) or 4 uppercase letters (ICAO).");

        final String dest = promptValidated(
                "Route destination airport code (IATA or ICAO)", "MAD",
                "3-letter IATA (e.g. MAD) or 4-letter ICAO (e.g. LEMD), uppercase only — must differ from origin",
                v -> AIRPORT.matcher(v).matches() && !v.equals(origin),
                "Must be 3-4 uppercase letters and must differ from the origin (" + origin + ").");

        sb.append("flight ").append(designator).append(" {\n");
        sb.append("    type:      ").append(type).append(";\n");
        sb.append("    aircraft:  ").append(aircraftId).append(";\n");
        sb.append("    pilot:     ").append(pilotEmail).append(";\n");
        sb.append("    route:     ").append(routeRef)
                .append(" from ").append(origin)
                .append(" to ").append(dest).append(";\n");

        System.out.println();
        System.out.println("  >>> Route constraint: first leg departure = '" + origin
                + "', last leg arrival = '" + dest + "' <<<");

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

    /**
     * leg: departure arrival fuel profile? climbSegment cruiseSegment
     * descendSegment
     */
    private void buildLeg(final StringBuilder sb) {
        // Compute defaults at call time so they are always in the future
        final LocalDateTime defaultDep = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);
        final LocalDateTime defaultArr = LocalDateTime.now().plusHours(2).plusMinutes(30).withSecond(0).withNano(0);
        buildDeparture(sb, defaultDep);
        buildArrival(sb, defaultArr);
        buildFuel(sb);
        if (confirm("Add optional flight profile (climb/descend tables)?", false)) {
            buildProfile(sb);
        }

        System.out.println();
        System.out.println("  -- Segment 1: Climb --");
        final String[] climbTo = buildSegment(sb, "climb", null);

        System.out.println();
        System.out.println("  -- Segment 2: Cruise (from auto-filled from Climb end) --");
        final String[] cruiseTo = buildSegment(sb, "cruise", climbTo);

        System.out.println();
        System.out.println("  -- Segment 3: Descend (from auto-filled from Cruise end) --");
        buildSegment(sb, "descend", cruiseTo);
    }

    /** departure: DEPARTURE LBRACE airport date time RBRACE */
    private void buildDeparture(final StringBuilder sb, final LocalDateTime defaultDatetime) {
        System.out.println();
        System.out.println("  Departure:");
        final String defaultDate = defaultDatetime.toLocalDate().toString();
        final String defaultTime = defaultDatetime.toLocalTime().toString();

        final String airport = promptValidated(
                "    Airport code", "OPO",
                "3-letter IATA (e.g. OPO) or 4-letter ICAO (e.g. LPPR), uppercase only",
                v -> AIRPORT.matcher(v).matches(),
                "Must be 3 uppercase letters (IATA) or 4 uppercase letters (ICAO).");

        final String date = promptValidated(
                "    Date", defaultDate,
                "YYYY-MM-DD — must be today (" + LocalDate.now() + ") or a future date",
                v -> {
                    if (!DATE_FMT.matcher(v).matches())
                        return false;
                    try {
                        return !LocalDate.parse(v).isBefore(LocalDate.now());
                    } catch (DateTimeParseException e) {
                        return false;
                    }
                },
                "Must be a valid date (YYYY-MM-DD) that is today or in the future.");

        final String time = promptValidated(
                "    Time", defaultTime,
                "HH:MM in 24-hour format (e.g. " + defaultTime + ")",
                v -> TIME_FMT.matcher(v).matches(),
                "Must be HH:MM — hours 00-23, minutes 00-59.");

        sb.append("        departure {\n")
                .append("            airport: ").append(airport).append(";\n")
                .append("            date:    ").append(date).append(";\n")
                .append("            time:    ").append(time).append(";\n")
                .append("        }\n");
    }

    /** arrival: ARRIVAL LBRACE airport date time RBRACE */
    private void buildArrival(final StringBuilder sb, final LocalDateTime defaultDatetime) {
        System.out.println();
        System.out.println("  Arrival:");
        final String defaultDate = defaultDatetime.toLocalDate().toString();
        final String defaultTime = defaultDatetime.toLocalTime().toString();

        final String airport = promptValidated(
                "    Airport code", "MAD",
                "3-letter IATA (e.g. MAD) or 4-letter ICAO (e.g. LEMD), uppercase only",
                v -> AIRPORT.matcher(v).matches(),
                "Must be 3 uppercase letters (IATA) or 4 uppercase letters (ICAO).");

        final String date = promptValidated(
                "    Date", defaultDate,
                "YYYY-MM-DD — must be today (" + LocalDate.now() + ") or a future date",
                v -> {
                    if (!DATE_FMT.matcher(v).matches())
                        return false;
                    try {
                        return !LocalDate.parse(v).isBefore(LocalDate.now());
                    } catch (DateTimeParseException e) {
                        return false;
                    }
                },
                "Must be a valid date (YYYY-MM-DD) that is today or in the future.");

        final String time = promptValidated(
                "    Time", defaultTime,
                "HH:MM in 24-hour format (e.g. " + defaultTime + ")",
                v -> TIME_FMT.matcher(v).matches(),
                "Must be HH:MM — hours 00-23, minutes 00-59.");

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
        final String qty = promptValidated(
                "    Quantity", "40200",
                "Positive number > 0 (e.g. 40200 or 18500.5)",
                v -> {
                    try {
                        return Double.parseDouble(v) > 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                },
                "Must be a positive number greater than 0.");
        final String unit = promptChoice("    Unit", new String[] { "kg", "l" }, "kg");
        sb.append("        fuel {\n")
                .append("            quantity: ").append(qty).append(" ").append(unit).append(";\n")
                .append("        }\n");
    }

    /**
     * profile: PROFILE LBRACE profileEntry+ RBRACE
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

            final String alt = promptValidated(
                    "      Altitude value", "1000",
                    "Non-negative number (>= 0), e.g. 1000",
                    v -> {
                        try {
                            return Double.parseDouble(v) >= 0;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    },
                    "Must be a non-negative number (>= 0).");
            final String altUnit = promptChoice("      Altitude unit", new String[] { "m", "ft" }, "m");

            final String spd = promptValidated(
                    "      Speed value", "210",
                    "Positive number > 0 (e.g. 210)",
                    v -> {
                        try {
                            return Double.parseDouble(v) > 0;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    },
                    "Must be a positive number greater than 0.");
            final String spdUnit = promptChoice("      Speed unit", new String[] { "kts", "m/s" }, "kts");

            if ("climb".equals(kind)) {
                sb.append("            climb   [ altitude: ").append(alt).append(" ").append(altUnit)
                        .append(", speed: ").append(spd).append(" ").append(spdUnit).append(" ];\n");
            } else {
                final String rate = promptValidated(
                        "      Rate of descent", "-10",
                        "Negative number < 0 (e.g. -10 or -7.5)",
                        v -> {
                            try {
                                return Double.parseDouble(v) < 0;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        },
                        "Must be a negative number (< 0).");
                sb.append("            descend [ altitude: ").append(alt).append(" ").append(altUnit)
                        .append(", speed: ").append(spd).append(" ").append(spdUnit)
                        .append(", rate: ").append(rate).append(" ];\n");
            }
        } while (confirm("    Add another profile entry?", false));
        sb.append("        }\n");
    }

    /**
     * Builds one segment block. If {@code prefilledFrom} is non-null it carries
     * [lat, lon, alt, altUnit] from the previous segment's "to" coordinate so the
     * user is not asked again (segment continuity). Returns the "to" coordinate as
     * [lat, lon, alt, altUnit] for the caller to pass to the next segment.
     */
    private String[] buildSegment(final StringBuilder sb, final String mode, final String[] prefilledFrom) {

        final String fromLat, fromLon, fromAlt, fromAltUnit;

        if (prefilledFrom != null) {
            fromLat = prefilledFrom[0];
            fromLon = prefilledFrom[1];
            fromAlt = prefilledFrom[2];
            fromAltUnit = prefilledFrom[3];
            System.out.printf("    From coordinate (auto-filled): lat=%-12s lon=%-12s alt=%s %s%n",
                    fromLat, fromLon, fromAlt, fromAltUnit);
        } else {
            System.out.println("    From coordinate:");
            fromLat = promptValidated("      Latitude", "41.262891",
                    "Decimal degrees, -90 to 90 (e.g. 41.262891)",
                    v -> {
                        try {
                            double d = Double.parseDouble(v);
                            return d >= -90 && d <= 90;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    },
                    "Must be a decimal number between -90 and 90.");
            fromLon = promptValidated("      Longitude", "-8.68522",
                    "Decimal degrees, -180 to 180 (e.g. -8.68522)",
                    v -> {
                        try {
                            double d = Double.parseDouble(v);
                            return d >= -180 && d <= 180;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    },
                    "Must be a decimal number between -180 and 180.");
            fromAlt = promptValidated("      Altitude value", "100",
                    "Non-negative number >= 0 (e.g. 100)",
                    v -> {
                        try {
                            return Double.parseDouble(v) >= 0;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    },
                    "Must be a non-negative number (>= 0).");
            fromAltUnit = promptChoice("      Altitude unit", new String[] { "m", "ft" }, "m");
        }

        final double fromAltNum = parseDouble(fromAlt);

        // Default "to" values differ per segment mode so the examples are always valid
        final String defaultToLat, defaultToLon, defaultToAlt;
        if ("climb".equals(mode)) {
            defaultToLat = "41.0";
            defaultToLon = "-7.0";
            defaultToAlt = "9000";
        } else if ("cruise".equals(mode)) {
            defaultToLat = "40.8";
            defaultToLon = "-5.0";
            defaultToAlt = fromAlt; // same altitude — level cruise
        } else { // descend
            defaultToLat = "40.4895";
            defaultToLon = "-3.5643";
            defaultToAlt = "100";
        }

        final String altHint;
        if ("climb".equals(mode)) {
            altHint = "Must be > " + fromAlt + " — climb must ascend";
        } else if ("descend".equals(mode)) {
            altHint = "Must be < " + fromAlt + " — descend must lose altitude";
        } else {
            altHint = "Non-negative number >= 0";
        }

        System.out.println("    To coordinate (must differ from From):");
        final String toLat = promptValidated("      Latitude", defaultToLat,
                "Decimal degrees, -90 to 90",
                v -> {
                    try {
                        double d = Double.parseDouble(v);
                        return d >= -90 && d <= 90;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                },
                "Must be a decimal number between -90 and 90.");
        final String toLon = promptValidated("      Longitude", defaultToLon,
                "Decimal degrees, -180 to 180",
                v -> {
                    try {
                        double d = Double.parseDouble(v);
                        return d >= -180 && d <= 180;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                },
                "Must be a decimal number between -180 and 180.");
        final String toAlt = promptValidated("      Altitude value", defaultToAlt, altHint,
                v -> {
                    try {
                        final double val = Double.parseDouble(v);
                        if (val < 0)
                            return false;
                        if ("climb".equals(mode) && val <= fromAltNum)
                            return false;
                        if ("descend".equals(mode) && val >= fromAltNum)
                            return false;
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                },
                "climb".equals(mode) ? "Must be a number > " + fromAlt + " (climb ascends)."
                        : "descend".equals(mode) ? "Must be a number < " + fromAlt + " (descend loses altitude)."
                                : "Must be a non-negative number (>= 0).");
        final String toAltUnit = promptChoice("      Altitude unit", new String[] { "m", "ft" }, fromAltUnit);

        sb.append("        segment {\n")
                .append("            from: (").append(fromLat).append(", ").append(fromLon)
                .append(", altitude: ").append(fromAlt).append(" ").append(fromAltUnit).append(");\n")
                .append("            to:   (").append(toLat).append(", ").append(toLon)
                .append(", altitude: ").append(toAlt).append(" ").append(toAltUnit).append(");\n");

        // Altitude slots (at least one required)
        int slotNum = 1;
        do {
            System.out.println("    Altitude slot " + slotNum + ":");
            final String minAlt = promptValidated("      Min altitude", "0",
                    "Non-negative number >= 0 (e.g. 0)",
                    v -> {
                        try {
                            return Double.parseDouble(v) >= 0;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    },
                    "Must be a non-negative number (>= 0).");
            final String minUnit = promptChoice("      Min unit", new String[] { "m", "ft" }, "m");
            final double minAltNum = parseDouble(minAlt);
            final String maxAlt = promptValidated("      Max altitude", "9249",
                    "Must be greater than min (" + minAlt + ")",
                    v -> {
                        try {
                            return Double.parseDouble(v) > minAltNum;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    },
                    "Must be greater than the min altitude (" + minAlt + ").");
            final String maxUnit = promptChoice("      Max unit", new String[] { "m", "ft" }, "m");
            final String width = promptValidated("      Width", "50",
                    "Positive number > 0 — corridor width (e.g. 50)",
                    v -> {
                        try {
                            return Double.parseDouble(v) > 0;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    },
                    "Must be a positive number greater than 0.");
            final String widthUnit = promptChoice("      Width unit", new String[] { "m", "ft" }, "m");
            sb.append("            altslot [ min: ").append(minAlt).append(" ").append(minUnit)
                    .append(", max: ").append(maxAlt).append(" ").append(maxUnit)
                    .append(", width: ").append(width).append(" ").append(widthUnit).append(" ];\n");
            slotNum++;
        } while (confirm("    Add another altitude slot?", false));

        System.out.println("    Wind:");
        final String windDir = promptValidated("      Direction (degrees)", "270",
                "0-359 — angle relative to North (e.g. 270 = West, 90 = East)",
                v -> {
                    try {
                        double d = Double.parseDouble(v);
                        return d >= 0 && d < 360;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                },
                "Must be a number between 0 and 359.");
        final String windSpd = promptValidated("      Speed (m/s)", "15",
                "Positive number, 0 < speed <= 25 m/s (operational limit)",
                v -> {
                    try {
                        double d = Double.parseDouble(v);
                        return d > 0 && d <= 25;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                },
                "Must be > 0 and <= 25 m/s.");

        sb.append("            wind { direction: ").append(windDir)
                .append("; speed: ").append(windSpd).append(" m/s; }\n");
        sb.append("            mode: ").append(mode).append(";\n");
        sb.append("        }\n");

        return new String[] { toLat, toLon, toAlt, toAltUnit };
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
                final FlightPlan saved = controller.importFlightPlan(
                        fp.flightDesignator(), fp.aircraftId(), fp.pilotRef(), legReqs);
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
     * Prompts repeatedly until the entered value satisfies {@code isValid}.
     * Pressing ENTER with no input uses {@code example} (which must be valid).
     */
    private String promptValidated(final String label, final String example, final String hint,
            final Predicate<String> isValid, final String errorMsg) {
        while (true) {
            System.out.println("  Hint: " + hint);
            final String raw = Console.readLine("  " + label + " [" + example + "]");
            final String value = (raw == null || raw.trim().isEmpty()) ? example : raw.trim();
            if (isValid.test(value)) {
                return value;
            }
            System.out.println("  ERROR: " + errorMsg);
            System.out.println();
        }
    }

    /** Prompts repeatedly until one of the allowed {@code options} is entered. */
    private String promptChoice(final String label, final String[] options, final String example) {
        final String opts = String.join(" | ", options);
        final Set<String> valid = new HashSet<>(Arrays.asList(options));
        while (true) {
            final String raw = Console.readLine("  " + label + " (" + opts + ") [" + example + "]");
            final String value = (raw == null || raw.trim().isEmpty()) ? example : raw.trim().toLowerCase();
            if (valid.contains(value)) {
                return value;
            }
            System.out.println("  ERROR: must be one of: " + opts);
        }
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

    private double parseDouble(final String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String headline() {
        return "Create Flight Plan";
    }
}
