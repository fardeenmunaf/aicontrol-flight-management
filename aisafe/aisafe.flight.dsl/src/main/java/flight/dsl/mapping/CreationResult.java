package flight.dsl.mapping;

import flightPlan.domain.FlightPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of a DSL creation run: the plans created and any errors encountered. */
public class CreationResult {

    private final List<FlightPlan> created = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    void addCreated(final FlightPlan plan) { created.add(plan); }
    void addError(final String error) { errors.add(error); }

    public List<FlightPlan> created() { return Collections.unmodifiableList(created); }
    public List<String> errors() { return Collections.unmodifiableList(errors); }
    public boolean hasErrors() { return !errors.isEmpty(); }
}
