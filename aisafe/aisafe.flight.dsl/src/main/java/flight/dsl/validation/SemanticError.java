    package flight.dsl.validation;

public class SemanticError {

    private final SemanticErrorType type;
    private final int line;
    private final int column;
    private final String message;

    public SemanticError(final SemanticErrorType type, final int line, final int column, final String message) {
        this.type = type;
        this.line = line;
        this.column = column;
        this.message = message;
    }

    public SemanticErrorType type() {
        return type;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return type + " [line " + line + ", column " + column + "]: " + message;
    }
}

