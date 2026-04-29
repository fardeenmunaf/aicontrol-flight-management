package user.domain;

public enum Role {
    PILOT("Pilot"),
    AIR_TRAFFIC_CONTROLLER("Air Traffic Controller"),
    FLIGHT_DISPATCHER("Flight Dispatcher"),
    MAINTENANCE_ENGINEER("Maintenance Engineer"),
    AIRPORT_MANAGER("Airport Manager"),
    SYSTEM_ADMINISTRATOR("System Administrator");

    private final String designation;

    Role(final String designation) {
        this.designation = designation;
    }

    public String designation() {
        return designation;
    }
}

