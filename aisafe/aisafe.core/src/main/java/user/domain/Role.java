package user.domain;

public enum Role {
    ADMIN("Admin"),
    BACKOFFICE_OPERATOR("Backoffice Operator"),
    AIR_TRANSPORT_COMPANY_COLLABORATOR("ATCC"),
    PILOT("Pilot"),
    FLIGHT_CONTROL_OPERATOR("FCO"),
    WEATHER_PERSON("Weather Person");

    private final String designation;

    Role(final String designation) {
        this.designation = designation;
    }

    public String designation() {
        return designation;
    }
}
