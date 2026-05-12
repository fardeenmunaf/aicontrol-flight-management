package flight.dsl;

import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        final FlightDslProcessor processor = new FlightDslProcessor();
        final FlightDslProcessingResult result = processor.process(Path.of("input.txt"));

        System.out.println("Flights parsed: " + result.summary().flightPlanCount());
        System.out.println("Legs parsed: " + result.summary().legCount());
        System.out.println("Segments parsed: " + result.summary().segmentCount());

        if (result.semanticErrors().isEmpty()) {
            System.out.println("No semantic errors detected.");
        } else {
            System.out.println("Semantic errors:");
            result.semanticErrors().forEach(error -> System.out.println(" - " + error));
        }
    }
}