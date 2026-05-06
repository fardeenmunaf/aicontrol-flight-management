package flight.dsl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {

    public static void main(String[] args) throws Exception {
        FlightDSLLexer lexer = new FlightDSLLexer(
                CharStreams.fromFileName("input.txt")
        );

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FlightDSLParser parser = new FlightDSLParser(tokens);

        ParseTree tree = parser.start();

        System.out.println(tree.toStringTree(parser));
    }
}