package inmemory;

/**
 * Manages initial data seeding for the in-memory environment.
 * Instantiated once to populate test/development data.
 *
 * @author LAPR4 Team
 */
public final class InMemoryInitializer {
    private static boolean initialized = false;

    private InMemoryInitializer() {
        // ensure utility
    }

    /**
     * Initialize in-memory repositories with seed data.
     * Thread-safe initialization ensuring single execution.
     */
    public static synchronized void init() {
        if (!initialized) {
            // Seed initial data for each aggregate as needed
            // Example: Add default engine models, airports, etc.
            initialized = true;
        }
    }
}
