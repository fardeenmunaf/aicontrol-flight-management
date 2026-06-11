package flightPlan.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FlightPlanValidationResult {

    private final List<String> dslErrors = new ArrayList<>();
    private final List<String> physicalFailures = new ArrayList<>();
    private boolean physicalCheckRan = false;

    public void addDslError(final String error) {
        dslErrors.add(error);
    }

    public void addPhysicalFailure(final String failure) {
        physicalFailures.add(failure);
    }

    public void markPhysicalCheckRan() {
        physicalCheckRan = true;
    }

    public boolean passed() {
        return dslErrors.isEmpty() && physicalCheckRan && physicalFailures.isEmpty();
    }

    public List<String> dslErrors() {
        return Collections.unmodifiableList(dslErrors);
    }

    public List<String> physicalFailures() {
        return Collections.unmodifiableList(physicalFailures);
    }

    public boolean physicalCheckRan() {
        return physicalCheckRan;
    }
}
