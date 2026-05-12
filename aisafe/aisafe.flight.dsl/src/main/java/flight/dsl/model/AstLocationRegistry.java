package flight.dsl.model;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public class AstLocationRegistry {

    private final Map<Object, SourceLocation> locations = new IdentityHashMap<>();

    public void register(final Object node, final int line, final int column) {
        locations.put(node, new SourceLocation(line, column));
    }

    public Optional<SourceLocation> locationOf(final Object node) {
        return Optional.ofNullable(locations.get(node));
    }
}

