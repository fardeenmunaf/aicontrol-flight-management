package javafxui.presentation.simulation;

public record SimStep(
        String callsign,
        String phase,
        double lat,
        double lon,
        double alt,
        double fuelRemaining
) {}
