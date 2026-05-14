package backoffice.presentation.user;

import eapli.framework.visitor.Visitor;
import user.domain.User;

class UserPrinter implements Visitor<User> {
    @Override
    public void visit(final User visitee) {
        String roles = visitee.roles().isEmpty() ? "No roles" :
                       String.join(", ", visitee.roles().stream().map(Object::toString).toArray(String[]::new));
        System.out.printf("ID: %s | Name: %-20s | Email: %-30s | Roles: %-20s | Status: %s%n",
                visitee.userId(), visitee.name(), visitee.email(), roles, visitee.status());
    }
}

