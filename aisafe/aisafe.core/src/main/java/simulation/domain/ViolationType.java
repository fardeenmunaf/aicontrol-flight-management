package simulation.domain;

/** Categorises a safety violation recorded during a simulation run. */
public enum ViolationType {
    /** Two aircraft violated minimum separation distance. */
    COLLISION,
    /** A single aircraft exhausted its fuel before reaching its destination. */
    FUEL_EXHAUSTION
}
