package flight.dsl.mapping;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import flight.dsl.ast.AltitudeSlotAst;
import flight.dsl.ast.FlightPlanAst;
import flight.dsl.ast.LegAst;
import flight.dsl.ast.SegmentAst;
import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;
import flight.dsl.validation.SemanticError;
import flightPlan.application.FlightPlanValidationResult;
import flightPlan.application.ValidateFlightPlanController;
import flightPlan.domain.FlightPlan;
import shared.domain.FlightPlanId;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates the full US085 validation pipeline:
 * 1. DSL re-validation (lexical, syntactic, semantic)
 * 2. Physical checks via the C flight_plan_validator binary
 * 3. Status update on success
 */
public class ValidateFlightPlanService {

    private final ValidateFlightPlanController controller;
    private final FlightDslProcessor processor;
    private final AirportRepository airportRepository;
    private final ObjectMapper objectMapper;

    public ValidateFlightPlanService(final ValidateFlightPlanController controller,
                                     final AirportRepository airportRepository) {
        this.controller = Objects.requireNonNull(controller);
        this.airportRepository = Objects.requireNonNull(airportRepository);
        this.processor = new FlightDslProcessor();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Lists DRAFT flight plans belonging to the logged-in pilot.
     */
    public Iterable<FlightPlan> draftPlansForPilot() {
        return controller.draftPlansForLoggedInPilot();
    }

    /**
     * Runs the full validation pipeline for the given flight plan ID.
     * Returns a result with DSL errors and/or physical failures.
     * If all checks pass, the flight plan status is updated to TESTED.
     */
    public FlightPlanValidationResult validate(final FlightPlanId planId) {
        final FlightPlanValidationResult result = new FlightPlanValidationResult();

        // AC5/AC7/AC8: auth, ownership, status checks (throws on failure)
        final FlightPlan plan = controller.findForValidation(planId);

        final String dslText = plan.dslText();
        if (dslText == null || dslText.isBlank()) {
            result.addDslError("This flight plan has no stored DSL text and cannot be re-validated.");
            return result;
        }

        // AC1: full DSL re-validation
        FlightDslProcessingResult parseResult;
        try {
            parseResult = processor.process(dslText);
        } catch (final RuntimeException e) {
            result.addDslError("DSL parse error: " + e.getMessage());
            return result;
        }

        if (!parseResult.isValid()) {
            for (final SemanticError error : parseResult.semanticErrors()) {
                result.addDslError(error.toString());
            }
            return result;
        }

        // Find the AST for this specific flight plan
        final FlightPlanAst ast = parseResult.document().flightPlans().stream()
                .filter(fp -> fp.flightDesignator().equals(plan.flightDesignator()))
                .findFirst()
                .orElse(parseResult.document().flightPlans().isEmpty()
                        ? null
                        : parseResult.document().flightPlans().get(0));

        if (ast == null) {
            result.addDslError("Could not locate flight plan '" + plan.flightDesignator() + "' in the stored DSL.");
            return result;
        }

        // Look up destination airport's minimum required arrival fuel
        final String destCode = plan.routeDestination();
        final Optional<Airport> destAirport = airportRepository.findByIataCode(destCode)
                .or(() -> airportRepository.findByIcaoCode(destCode));

        double minRequiredFuel = 0.0;
        if (destAirport.isPresent() && destAirport.get().airControlArea() != null) {
            minRequiredFuel = destAirport.get().airControlArea().minimumRequiredArrivalFuel();
        }

        // AC2: build JSON input for C component and invoke it
        try {
            final File inputFile = buildValidatorInput(ast, minRequiredFuel);
            try {
                runCValidator(inputFile, result);
            } finally {
                inputFile.delete();
            }
        } catch (final IOException e) {
            result.addPhysicalFailure("Physical validation could not be executed: " + e.getMessage());
            result.markPhysicalCheckRan();
        }

        // AC6: update status to TESTED if all checks passed
        if (result.passed()) {
            controller.markAsTested(plan);
        }

        return result;
    }

    private File buildValidatorInput(final FlightPlanAst ast, final double minRequiredFuel) throws IOException {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("flightDesignator", ast.flightDesignator());
        root.put("minimumRequiredArrivalFuel", minRequiredFuel);

        final ArrayNode legsNode = root.putArray("legs");
        final List<LegAst> legs = ast.legs();
        for (int li = 0; li < legs.size(); li++) {
            final LegAst leg = legs.get(li);
            final ObjectNode legNode = legsNode.addObject();
            legNode.put("legNumber", li + 1);
            legNode.put("fuelQuantity", leg.fuel().quantity());

            final ArrayNode segsNode = legNode.putArray("segments");
            final List<SegmentAst> segments = leg.segments();
            for (int si = 0; si < segments.size(); si++) {
                final SegmentAst seg = segments.get(si);
                final ObjectNode segNode = segsNode.addObject();
                segNode.put("segmentNumber", si + 1);
                segNode.put("mode", seg.mode() != null ? seg.mode().name().toLowerCase() : "cruise");
                segNode.put("fromAltitude", seg.from().altitude());
                segNode.put("toAltitude", seg.to().altitude());
                segNode.put("altitudeUnit",
                        seg.from().altitudeUnit() != null ? seg.from().altitudeUnit() : "m");

                // Use the first altitude slot's min/max if present
                if (!seg.altitudeSlots().isEmpty()) {
                    final AltitudeSlotAst slot = seg.altitudeSlots().get(0);
                    segNode.put("minimumAltitude", slot.min());
                    segNode.put("maximumAltitude", slot.max());
                } else {
                    segNode.put("minimumAltitude", 0.0);
                    segNode.put("maximumAltitude", Double.MAX_VALUE);
                }
            }
        }

        final File tmp = File.createTempFile("fp_validate_", ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp, root);
        return tmp;
    }

    private void runCValidator(final File inputFile, final FlightPlanValidationResult result) throws IOException {
        final String binaryPath = locateValidatorBinary();
        final ProcessBuilder pb = new ProcessBuilder(binaryPath, inputFile.getAbsolutePath());
        pb.redirectErrorStream(false);

        final Process process;
        try {
            process = pb.start();
        } catch (final IOException e) {
            result.addPhysicalFailure(
                    "C validator binary not found or could not be launched (" + binaryPath + "): " + e.getMessage());
            result.markPhysicalCheckRan();
            return;
        }

        final StringBuilder stdout = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stdout.append(line).append("\n");
            }
        }

        try {
            process.waitFor();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            result.addPhysicalFailure("Physical validation was interrupted.");
            result.markPhysicalCheckRan();
            return;
        }

        result.markPhysicalCheckRan();
        parseValidatorOutput(stdout.toString(), result);
    }

    private void parseValidatorOutput(final String output, final FlightPlanValidationResult result) {
        if (output == null || output.isBlank()) {
            result.addPhysicalFailure("C validator produced no output.");
            return;
        }
        try {
            final com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(output);
            final com.fasterxml.jackson.databind.JsonNode failures = root.get("failures");
            if (failures != null && failures.isArray()) {
                for (final com.fasterxml.jackson.databind.JsonNode f : failures) {
                    final com.fasterxml.jackson.databind.JsonNode desc = f.get("description");
                    result.addPhysicalFailure(desc != null ? desc.asText() : f.toString());
                }
            }
        } catch (final Exception e) {
            result.addPhysicalFailure("Could not parse C validator output: " + e.getMessage());
        }
    }

    private static String locateValidatorBinary() {
        final String[] candidates = {
            "aisafe-simulation/flight_plan_validator",
            "../aisafe-simulation/flight_plan_validator",
            "flight_plan_validator"
        };
        for (final String candidate : candidates) {
            if (new File(candidate).exists()) {
                return candidate;
            }
        }
        // Binary not found — try to build it automatically with make
        final String[] simDirs = { "aisafe-simulation", "../aisafe-simulation" };
        for (final String simDir : simDirs) {
            final File dir = new File(simDir);
            if (dir.isDirectory()) {
                try {
                    final Process p = new ProcessBuilder("make", "flight_plan_validator")
                            .directory(dir)
                            .redirectErrorStream(true)
                            .start();
                    p.waitFor();
                    for (final String candidate : candidates) {
                        if (new File(candidate).exists()) {
                            return candidate;
                        }
                    }
                } catch (final Exception ignored) {
                }
            }
        }
        return candidates[0];
    }
}
