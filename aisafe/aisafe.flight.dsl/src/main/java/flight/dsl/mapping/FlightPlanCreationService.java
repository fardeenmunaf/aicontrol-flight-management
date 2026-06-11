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

    public FlightPlanCreationService(final FlightPlanRepository flightPlanRepository,
                                     final AirportRepository airportRepository,
                                     final NodeRepository nodeRepository) {
        this.mapper = new FlightPlanAstMapper(airportRepository, nodeRepository);
        this.controller = new CreateFlightPlanController(flightPlanRepository);
    }

    public CreationResult createFromText(final String dslText) {
        try {
            return persist(processor.process(dslText));
        } catch (final RuntimeException e) {
            return errorResult("DSL parse error: " + e.getMessage());
        }
    }

    public CreationResult createFromFile(final Path dslFile) throws IOException {
        try {
            return persist(processor.process(dslFile));
        } catch (final RuntimeException e) {
            return errorResult("DSL parse error: " + e.getMessage());
        }
    }

    private static CreationResult errorResult(final String message) {
        final CreationResult result = new CreationResult();
        result.addError(message);
        return result;
    }

    private CreationResult persist(final FlightDslProcessingResult parseResult) {
        final CreationResult result = new CreationResult();
        if (!parseResult.isValid()) {
            parseResult.semanticErrors().forEach(e -> result.addError(e.toString()));
            return result;
        }
        for (final FlightPlanAst ast : parseResult.document().flightPlans()) {
            try {
                result.addCreated(controller.create(mapper.map(ast)));
            } catch (final Exception e) {
                result.addError(ast.flightDesignator() + ": " + e.getMessage());
            }
        }
        return result;
    }
}
