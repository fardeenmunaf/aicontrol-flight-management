package flight.dsl.mapping;

import flightPlan.domain.FlightPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of a DSL creation run: the plans created, any errors, and non-fatal warnings. */
public class CreationResult {

    private final List<FlightPlan> created = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    void addCreated(final FlightPlan plan) { created.add(plan); }
    void addError(final String error) { errors.add(error); }
    void addWarning(final String warning) { warnings.add(warning); }

    public List<FlightPlan> created() { return Collections.unmodifiableList(created); }
    public List<String> errors() { return Collections.unmodifiableList(errors); }
    public List<String> warnings() { return Collections.unmodifiableList(warnings); }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
}
