package optimized.runner;

/** Immutable batch summary allocated once after measurement, never per turn. */
public record SimulationStats(
        int games,
        long elapsedNanos,
        double gamesPerSecond,
        double meanTurns,
        double medianTurns,
        int minTurns,
        int maxTurns) {}
