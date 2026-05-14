package flight.dsl.model;

import flight.dsl.validation.SemanticError;

import java.util.List;
import java.util.Objects;

public class FlightDslProcessingResult {

    private final FlightDslDocument document;
    private final FlightStructureSummary summary;
    private final List<SemanticError> semanticErrors;

    public FlightDslProcessingResult(final FlightDslDocument document,
                                     final FlightStructureSummary summary,
                                     final List<SemanticError> semanticErrors) {
        this.document = Objects.requireNonNull(document, "document");
        this.summary = Objects.requireNonNull(summary, "summary");
        this.semanticErrors = List.copyOf(Objects.requireNonNull(semanticErrors, "semanticErrors"));
    }

    public FlightDslDocument document() {
        return document;
    }

    public FlightStructureSummary summary() {
        return summary;
    }

    public List<SemanticError> semanticErrors() {
        return semanticErrors;
    }

    public boolean isValid() {
        return semanticErrors.isEmpty();
    }
}

