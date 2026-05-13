package flight.dsl;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FlightDSLTest {

    private static class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            throw new ParseCancellationException(
                    "line " + line + ":" + charPositionInLine + " " + msg
            );
        }
    }

    private FlightDSLParser parserFor(String resourceName) {
        InputStream input = getClass().getResourceAsStream("/" + resourceName);
        if (input == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
        }

        try {
            CharStream chars = CharStreams.fromStream(input);

            FlightDSLLexer lexer = new FlightDSLLexer(chars);
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowingErrorListener());

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            FlightDSLParser parser = new FlightDSLParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowingErrorListener());

            return parser;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + resourceName, e);
        }
    }

    private void parseValid(String resourceName) {
        FlightDSLParser parser = parserFor(resourceName);
        assertDoesNotThrow(parser::start);
    }

    private void parseInvalid(String resourceName) {
        FlightDSLParser parser = parserFor(resourceName);
        assertThrows(ParseCancellationException.class, parser::start);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "valid_flight.dsl"
    })
    void shouldParseSyntacticallyValidFiles(String resourceName) {
        parseValid(resourceName);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid_missing_fuel.dsl",
            "invalid_missing_segment.dsl",
            "invalid_bad_date.dsl",
            "invalid_private_type.dsl",
            "invalid_negative_fuel.dsl"
    })
    void shouldRejectLexicallyOrSyntacticallyInvalidFiles(String resourceName) {
        parseInvalid(resourceName);
    }
}