package bootstrap;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Username;

import auth.domain.AISafeRoles;
import shared.domain.UserId;
import user.domain.Role;
import user.domain.User;
import user.repository.UserRepository;

import java.util.Set;

public class UsersBootstrap {

    private final UserRepository userRepository;

    public UsersBootstrap(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute() {
        registerIfAbsent("admin@aisafe.com",  "Admin1234!",    "System Admin",    AISafeRoles.ADMIN, Role.ADMIN, null);
        registerIfAbsent("bo@aisafe.com",     "Operator1234!", "Backoffice Ops",  AISafeRoles.BACKOFFICE_OPERATOR, Role.BACKOFFICE_OPERATOR, null);
        registerIfAbsent("pilot@tap.com",     "Pilot1234!",    "TAP Pilot",       AISafeRoles.PILOT, Role.PILOT, null);
        registerIfAbsent("fco@aisafe.com",    "Fco1234!",      "Default FCO",     AISafeRoles.FLIGHT_CONTROL_OPERATOR, Role.FLIGHT_CONTROL_OPERATOR, null);
        registerIfAbsent("weather@aisafe.com","Weather1234!",  "Weather Person",  AISafeRoles.WEATHER_PERSON, Role.WEATHER_PERSON, null);
        
    }

    private void registerIfAbsent(final String email, final String password,
                                  final String name,
                                  final eapli.framework.infrastructure.authz.domain.model.Role role,
                                  final Role domainRole,
                                  final Object unused) {
        if (AuthzRegistry.userService().userOfIdentity(Username.valueOf(email)).isEmpty()) {
            AuthzRegistry.userService().registerNewUser(
                    email,
                    password,
                    name,
                    "User",
                    email,
                    Set.of(role)
            );

            final User domainUser = new User(
                    email, name, "+351000000000",
                    null, null, "ACTIVE",
                    Set.of(domainRole),
                    UserId.valueOf(email)
            );

            userRepository.save(domainUser);
        }
    }
}