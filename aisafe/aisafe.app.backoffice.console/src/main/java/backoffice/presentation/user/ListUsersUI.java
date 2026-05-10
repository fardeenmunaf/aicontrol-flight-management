package backoffice.presentation.user;

import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;

import user.application.ListUsersController;
import user.domain.User;
import infrastructure.persistence.PersistenceContext;

public class ListUsersUI extends AbstractListUI<User> {

    private final ListUsersController controller =
            new ListUsersController(PersistenceContext.repositories().users());

    @Override
    protected Iterable<User> elements() {
        return controller.allUsers();
    }

    @Override
    protected Visitor<User> elementPrinter() {
        return user -> System.out.printf("%-30s | %-20s | %-20s | %-20s%n",
                user.email(),
                user.name(),
                user.phoneNumber(),
                user.status());
    }

    @Override
    protected String elementName() {
        return "User";
    }

    @Override
    protected String listHeader() {
        return String.format("%-30s | %-20s | %-20s | %-20s", "EMAIL", "NAME", "PHONE", "STATUS");
    }

    @Override
    protected String emptyMessage() {
        return "No Users found.";
    }

    @Override
    public String headline() {
        return "List All Users";
    }
}

