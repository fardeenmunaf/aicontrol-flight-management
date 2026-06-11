package flightPlan.application;

public final class AddWeatherResult {

    private final boolean testWasVoided;
    private final int addedCount;

    public AddWeatherResult(final boolean testWasVoided, final int addedCount) {
        this.testWasVoided = testWasVoided;
        this.addedCount = addedCount;
    }

    public boolean testWasVoided() {
        return testWasVoided;
    }

    public int addedCount() {
        return addedCount;
    }
}
