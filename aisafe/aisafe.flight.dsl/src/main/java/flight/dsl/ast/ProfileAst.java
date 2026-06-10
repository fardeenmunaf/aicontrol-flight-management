package flight.dsl.ast;

import java.util.List;
import java.util.Objects;

public class ProfileAst {

    private final List<ClimbEntryAst> climbEntries;
    private final List<CruiseEntryAst> cruiseEntries;
    private final List<DescendEntryAst> descendEntries;

    public ProfileAst(final List<ClimbEntryAst> climbEntries, final List<CruiseEntryAst> cruiseEntries, final List<DescendEntryAst> descendEntries) {
        this.climbEntries = List.copyOf(Objects.requireNonNull(climbEntries, "climbEntries"));
        this.cruiseEntries = List.copyOf(Objects.requireNonNull(cruiseEntries, "cruiseEntries"));
        this.descendEntries = List.copyOf(Objects.requireNonNull(descendEntries, "descendEntries"));
    }

    public List<ClimbEntryAst> climbEntries() {
        return climbEntries;
    }

    public List<CruiseEntryAst> cruiseEntries() {
        return cruiseEntries;
    }

    public List<DescendEntryAst> descendEntries() {
        return descendEntries;
    }
}
