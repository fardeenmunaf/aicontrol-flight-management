package flight.dsl.mapping;

import airport.repositories.AirportRepository;
import flight.dsl.ast.FlightPlanAst;
import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;
import flightPlan.application.CreateFlightPlanController;
import flightPlan.repository.FlightPlanRepository;
import node.repository.NodeRepository;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Single entry point for DSL-driven flight plan creation. Parses + validates the DSL,
 * then maps and persists each valid plan. Used by both the console and JavaFX apps.
 */
public class FlightPlanCreationService {

    private final FlightDslProcessor processor = new FlightDslProcessor();
    private final FlightPlanAstMapper mapper;
    private final CreateFlightPlanController controller;
    private final aircraft.repositories.AircraftRepository aircraftRepository;

    public FlightPlanCreationService(final FlightPlanRepository flightPlanRepository,
                                     final AirportRepository airportRepository,
                                     final NodeRepository nodeRepository,
                                     final aircraft.repositories.AircraftRepository aircraftRepository) {
        this.mapper = new FlightPlanAstMapper(airportRepository, nodeRepository);
        this.controller = new CreateFlightPlanController(flightPlanRepository);
        this.aircraftRepository = aircraftRepository;
    }

    public CreationResult createFromText(final String dslText) {
        try {
            return persist(processor.process(dslText), dslText);
        } catch (final RuntimeException e) {
            return errorResult("DSL parse error: " + e.getMessage());
        }
    }

    public CreationResult createFromFile(final Path dslFile) throws IOException {
        final String dslText;
        try {
            dslText = java.nio.file.Files.readString(dslFile);
        } catch (final IOException e) {
            throw e;
        }
        try {
            return persist(processor.process(dslFile), dslText);
        } catch (final RuntimeException e) {
            return errorResult("DSL parse error: " + e.getMessage());
        }
    }

    private static CreationResult errorResult(final String message) {
        final CreationResult result = new CreationResult();
        result.addError(message);
        return result;
    }

    private CreationResult persist(final FlightDslProcessingResult parseResult, final String dslText) {
        final CreationResult result = new CreationResult();
        if (!parseResult.isValid()) {
            parseResult.semanticErrors().forEach(e -> result.addError(e.toString()));
            return result;
        }
        ExportSimulationFlightPlanService exporter = new ExportSimulationFlightPlanService(this.aircraftRepository, "aisafe/aisafe-simulation/flightplansSim");

        for (final FlightPlanAst ast : parseResult.document().flightPlans()) {
            try {
                final flightPlan.domain.FlightPlan plan = mapper.map(ast);
                plan.storeDslText(dslText);
                result.addCreated(controller.create(plan));
                try {
                    exporter.export(ast);
                } catch (Exception e) {
                    result.addWarning("JSON Export Failed for " + ast.flightDesignator() + ": " + e.getMessage());
                }
            } catch (final Exception e) {
                result.addError(ast.flightDesignator() + ": " + e.getMessage());
            }
        }
        return result;
    }
}
