package flight.dsl;

import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java -jar aisafe.flight.dsl.jar <path-to-dsl-file>");
            System.exit(1);
        }

        final Path file = Path.of(args[0]);
        final FlightDslProcessor processor = new FlightDslProcessor();

        FlightDslProcessingResult result;
        try {
            result = processor.process(file);
        } catch (ParseCancellationException e) {
            System.err.println("Lexical/Syntactic error: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Flights  : " + result.summary().flightPlanCount());
        System.out.println("Legs     : " + result.summary().legCount());
        System.out.println("Segments : " + result.summary().segmentCount());

        if (result.semanticErrors().isEmpty()) {
            System.out.println("OK — no semantic errors.");
        } else {
            System.out.println("Semantic errors (" + result.semanticErrors().size() + "):");
            result.semanticErrors().forEach(e -> System.out.println("  " + e));
        }
    }
}
