package flight.dsl.processing;

import flight.dsl.FlightDSLLexer;
import flight.dsl.FlightDSLParser;
import flight.dsl.listener.FlightStructureListener;
import flight.dsl.model.FlightDslDocument;
import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.validation.FlightSemanticValidator;
import flight.dsl.validation.SemanticError;
import flight.dsl.visitor.FlightAstBuilderVisitor;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class FlightDslProcessor {

    public FlightDslProcessingResult process(final Path file) throws IOException {
        return process(CharStreams.fromPath(file));
    }

    public FlightDslProcessingResult process(final String text) {
        return process(CharStreams.fromString(text));
    }

    public FlightDslProcessingResult process(final CharStream chars) {
        final FlightDSLLexer lexer = new FlightDSLLexer(chars);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener());

        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final FlightDSLParser parser = new FlightDSLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener());

        final ParseTree tree = parser.start();

        final FlightStructureListener listener = new FlightStructureListener();
        ParseTreeWalker.DEFAULT.walk(listener, tree);

        final FlightAstBuilderVisitor visitor = new FlightAstBuilderVisitor();
        final FlightDslDocument document = (FlightDslDocument) visitor.visit(tree);

        final FlightSemanticValidator validator = new FlightSemanticValidator();
        final List<SemanticError> semanticErrors = validator.validate(document);

        return new FlightDslProcessingResult(document, listener.summary(), semanticErrors);
    }

    private static class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer,
                                final Object offendingSymbol,
                                final int line,
                                final int charPositionInLine,
                                final String msg,
                                final RecognitionException e) {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }
}

