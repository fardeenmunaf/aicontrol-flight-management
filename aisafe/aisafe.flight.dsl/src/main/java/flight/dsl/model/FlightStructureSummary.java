package flight.dsl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlightStructureSummary {

    private int flightPlanCount;
    private int legCount;
    private int segmentCount;
    private int profileCount;
    private final List<String> flightDesignators = new ArrayList<>();

    public void incrementFlightPlanCount() {
        flightPlanCount++;
    }

    public void incrementLegCount() {
        legCount++;
    }

    public void incrementSegmentCount() {
        segmentCount++;
    }

    public void incrementProfileCount() {
        profileCount++;
    }

    public void addFlightDesignator(final String designator) {
        flightDesignators.add(designator);
    }

    public int flightPlanCount() {
        return flightPlanCount;
    }

    public int legCount() {
        return legCount;
    }

    public int segmentCount() {
        return segmentCount;
    }

    public int profileCount() {
        return profileCount;
    }

    public List<String> flightDesignators() {
        return Collections.unmodifiableList(flightDesignators);
    }
}

