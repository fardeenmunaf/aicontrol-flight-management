package auth.domain;

import eapli.framework.infrastructure.authz.domain.model.Role;

public final class AISafeRoles {

    public static final Role ADMIN =
            Role.valueOf("ADMIN");
    public static final Role BACKOFFICE_OPERATOR =
            Role.valueOf("BACKOFFICE_OPERATOR");
    public static final Role AIR_TRANSPORT_COMPANY_COLLABORATOR =
            Role.valueOf("ATCC");
    public static final Role PILOT =
            Role.valueOf("PILOT");
    public static final Role FLIGHT_CONTROL_OPERATOR =
            Role.valueOf("FCO");
    public static final Role WEATHER_PERSON =
            Role.valueOf("WEATHER_PERSON");

    private AISafeRoles() {}
}