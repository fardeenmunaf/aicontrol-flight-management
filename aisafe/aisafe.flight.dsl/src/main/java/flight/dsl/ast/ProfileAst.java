package flight.dsl.ast;

import java.util.List;
import java.util.Objects;

public class ProfileAst {

    private final List<ClimbEntryAst> climbEntries;
    private final List<DescendEntryAst> descendEntries;

    public ProfileAst(final List<ClimbEntryAst> climbEntries, final List<DescendEntryAst> descendEntries) {
        this.climbEntries = List.copyOf(Objects.requireNonNull(climbEntries, "climbEntries"));
        this.descendEntries = List.copyOf(Objects.requireNonNull(descendEntries, "descendEntries"));
    }

    public List<ClimbEntryAst> climbEntries() {
        return climbEntries;
    }

    public List<DescendEntryAst> descendEntries() {
        return descendEntries;
    }
}

