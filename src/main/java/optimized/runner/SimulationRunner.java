package optimized.runner;

import java.util.Arrays;
import java.util.SplittableRandom;
import optimized.engine.OptimizedGame;
import optimized.strategies.OptimizedStrategy;

/** Single-thread worker that reuses one game and one strategy across all games. */
public final class SimulationRunner {
    private final OptimizedGame game = new OptimizedGame();
    private final OptimizedStrategy strategy;
    private SplittableRandom random;

    public SimulationRunner(OptimizedStrategy strategy, long seed) {
        this.strategy = strategy;
        this.random = new SplittableRandom(seed);
    }

    public void reseed(long seed) {
        random = new SplittableRandom(seed);
    }

    public int playOne() {
        game.resetRandom(random);
        strategy.reset(random);
        int turns = 0;
        while (!game.isComplete()) {
            int shot = strategy.chooseShot();
            int result = game.shoot(shot);
            strategy.trackShot(shot, result, game.lastSunkLo(), game.lastSunkHi());
            turns++;
        }
        return turns;
    }

    public SimulationStats run(int games) {
        if (games <= 0) throw new IllegalArgumentException("games must be positive");
        int[] turns = new int[games];
        long totalTurns = 0L;
        int min = 100;
        int max = 0;
        long start = System.nanoTime();
        for (int gameIndex = 0; gameIndex < games; gameIndex++) {
            int value = playOne();
            turns[gameIndex] = value;
            totalTurns += value;
            if (value < min) min = value;
            if (value > max) max = value;
        }
        long elapsed = System.nanoTime() - start;
        Arrays.sort(turns);
        double median = (games & 1) == 0
                ? (turns[games / 2 - 1] + turns[games / 2]) / 2.0
                : turns[games / 2];
        return new SimulationStats(
                games,
                elapsed,
                games * 1_000_000_000.0 / elapsed,
                (double) totalTurns / games,
                median,
                min,
                max);
    }
}
