package optimized.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;
import legacy.compstrategies.Parity;
import legacy.compstrategies.standardstrats.RandomHuntStrat;
import legacy.compstrategies.standardstrats.RandomStrat;
import legacy.gamefiles.Ocean;
import legacy.gamefiles.Ship;
import optimized.engine.BitBoard100;
import optimized.engine.OptimizedGame;
import optimized.runner.SimulationRunner;
import optimized.runner.SimulationStats;
import optimized.strategies.OptimizedStrategy;
import optimized.strategies.ParityStrategy;
import optimized.strategies.RandomHuntStrategy;
import optimized.strategies.RandomStrategy;

/** Controlled single-thread macrobenchmark; no output occurs inside measured loops. */
public final class Phase1Benchmark {
    private static volatile long blackhole;

    private enum Kind { RANDOM, PARITY, RANDOM_HUNT }

    public static void main(String[] args) throws Exception {
        int warmupGames = Integer.getInteger("benchmark.warmupGames", 2_000);
        int games = Integer.getInteger("benchmark.games", 10_000);
        int trials = Integer.getInteger("benchmark.trials", 5);

        System.out.printf("Phase 1 single-thread benchmark: warmup=%d, games/trial=%d, trials=%d%n",
                warmupGames, games, trials);
        System.out.println("implementation,strategy,games,time_ms,games_per_sec,mean,median,min,max");
        for (Kind kind : Kind.values()) {
            benchmarkLegacy(kind, warmupGames, games, trials);
            benchmarkOptimized(kind, warmupGames, games, trials);
        }
        benchmarkCoreOperations();
    }

    private static void benchmarkLegacy(Kind kind, int warmupGames, int games, int trials) throws Exception {
        runLegacy(kind, warmupGames);
        SimulationStats[] results = new SimulationStats[trials];
        for (int trial = 0; trial < trials; trial++) results[trial] = runLegacy(kind, games);
        print("legacy", displayName(kind), medianTrial(results));
    }

    private static void benchmarkOptimized(Kind kind, int warmupGames, int games, int trials) {
        OptimizedStrategy strategy = switch (kind) {
            case RANDOM -> new RandomStrategy();
            case PARITY -> new ParityStrategy();
            case RANDOM_HUNT -> new RandomHuntStrategy();
        };
        SimulationRunner runner = new SimulationRunner(strategy, 0xB4771E5L);
        runner.run(warmupGames);
        SimulationStats[] results = new SimulationStats[trials];
        for (int trial = 0; trial < trials; trial++) {
            runner.reseed(0xB4771E5L + trial);
            results[trial] = runner.run(games);
        }
        print("optimized", displayName(kind), medianTrial(results));
    }

    private static SimulationStats runLegacy(Kind kind, int games) throws Exception {
        int[] turns = new int[games];
        long total = 0L;
        int min = 100;
        int max = 0;
        long start = System.nanoTime();
        for (int game = 0; game < games; game++) {
            int value = playLegacy(kind);
            turns[game] = value;
            total += value;
            if (value < min) min = value;
            if (value > max) max = value;
        }
        long elapsed = System.nanoTime() - start;
        Arrays.sort(turns);
        double median = (games & 1) == 0
                ? (turns[games / 2 - 1] + turns[games / 2]) / 2.0
                : turns[games / 2];
        return new SimulationStats(games, elapsed, games * 1_000_000_000.0 / elapsed,
                (double) total / games, median, min, max);
    }

    /** Mirrors the frozen Game loop while selecting one of its preserved simple strategies. */
    private static int playLegacy(Kind kind) throws Exception {
        Ocean ocean = new Ocean(10);
        ocean.placeRandomBoats();
        ArrayList<Ship> ships = ocean.getBoats();
        boolean[] reportedSunk = new boolean[5];
        RandomStrat random = kind == Kind.RANDOM ? new RandomStrat(10) : null;
        Parity parity = kind == Kind.PARITY ? new Parity(10) : null;
        RandomHuntStrat randomHunt = kind == Kind.RANDOM_HUNT ? new RandomHuntStrat(10) : null;
        int turns = 0;

        while (!ocean.isAllSunk()) {
            int shot = switch (kind) {
                case RANDOM -> random.selectShot();
                case PARITY -> parity.selectShot();
                case RANDOM_HUNT -> randomHunt.selectShot();
            };
            boolean hit = ocean.isHit(shot);
            if (hit) {
                for (Ship ship : ships) {
                    int[] spots = ship.getSpots();
                    for (int index = 0; index < spots.length; index++) {
                        if (spots[index] == shot) spots[index] = -1;
                    }
                }
            }
            int sunkLength = 0;
            int[] sunkCells = null;
            for (int ship = 0; ship < ships.size(); ship++) {
                if (!reportedSunk[ship] && ships.get(ship).isSunk()) {
                    reportedSunk[ship] = true;
                    sunkLength = ships.get(ship).getBoatLength();
                    sunkCells = ships.get(ship).getSaveSpots();
                    break;
                }
            }
            switch (kind) {
                case RANDOM -> random.trackShot(hit, sunkLength, shot, sunkCells);
                case PARITY -> parity.trackShot(hit, sunkLength, shot, sunkCells);
                case RANDOM_HUNT -> randomHunt.trackShot(hit, sunkLength, shot, sunkCells);
            }
            turns++;
        }
        return turns;
    }

    private static SimulationStats medianTrial(SimulationStats[] results) {
        Arrays.sort(results, Comparator.comparingLong(SimulationStats::elapsedNanos));
        return results[results.length / 2];
    }

    private static void print(String implementation, String strategy, SimulationStats result) {
        System.out.printf("%s,%s,%d,%.3f,%.1f,%.3f,%.1f,%d,%d%n",
                implementation, strategy, result.games(), result.elapsedNanos() / 1_000_000.0,
                result.gamesPerSecond(), result.meanTurns(), result.medianTurns(),
                result.minTurns(), result.maxTurns());
    }

    private static String displayName(Kind kind) {
        return switch (kind) {
            case RANDOM -> "Random";
            case PARITY -> "Parity";
            case RANDOM_HUNT -> "RandomHunt";
        };
    }

    private static void benchmarkCoreOperations() throws Exception {
        final int operations = 1_000_000;
        OptimizedGame game = new OptimizedGame();
        SplittableRandom random = new SplittableRandom(1234);
        long[] lo = fleetLow();
        long[] hi = fleetHigh();

        long start = System.nanoTime();
        for (int i = 0; i < operations; i++) game.resetRandom(random);
        long fleetNanos = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < operations; i++) game.resetWithFleet(lo, hi);
        long resetNanos = System.nanoTime() - start;

        game.resetWithFleet(lo, hi);
        start = System.nanoTime();
        for (int i = 0; i < operations; i++) blackhole ^= game.isComplete() ? 1 : 0;
        long completeNanos = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < operations; i++) blackhole ^= game.isShipSunk(i % 5) ? 1 : 0;
        long sunkNanos = System.nanoTime() - start;

        long overlapLo = lo[0];
        long overlapHi = hi[0];
        start = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            blackhole ^= ((overlapLo & lo[i % 5]) | (overlapHi & hi[i % 5]));
        }
        long overlapNanos = System.nanoTime() - start;

        int[] shots = {0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 20, 30, 63, 64, 65, 90, 91};
        int shotBatches = operations / shots.length;
        start = System.nanoTime();
        for (int i = 0; i < shotBatches; i++) {
            game.resetWithFleet(lo, hi);
            for (int shot : shots) blackhole ^= game.shoot(shot);
        }
        long shotNanos = System.nanoTime() - start;

        int legacyFleetOperations = 100_000;
        start = System.nanoTime();
        for (int i = 0; i < legacyFleetOperations; i++) {
            Ocean ocean = new Ocean(10);
            ocean.placeRandomBoats();
            blackhole ^= ocean.getBoats().size();
        }
        long legacyFleetNanos = System.nanoTime() - start;

        System.out.println("core_operation,iterations,total_ms,ns_per_operation");
        coreLine("legacy_fleet_generation", legacyFleetOperations, legacyFleetNanos);
        coreLine("fleet_generation", operations, fleetNanos);
        coreLine("fixed_state_reset", operations, resetNanos);
        coreLine("game_over_check", operations, completeNanos);
        coreLine("ship_sunk_check", operations, sunkNanos);
        coreLine("overlap_check", operations, overlapNanos);
        coreLine("shot_resolution", (long) shotBatches * shots.length, shotNanos);
    }

    private static void coreLine(String name, long operations, long nanos) {
        System.out.printf("%s,%d,%.3f,%.3f%n", name, operations, nanos / 1_000_000.0,
                (double) nanos / operations);
    }

    private static long[] fleetLow() {
        return new long[] {maskLow(0, 1, 2, 3, 4), maskLow(6, 7, 8, 9),
                maskLow(10, 20, 30), maskLow(63, 64, 65), maskLow(90, 91)};
    }

    private static long[] fleetHigh() {
        return new long[] {maskHigh(0, 1, 2, 3, 4), maskHigh(6, 7, 8, 9),
                maskHigh(10, 20, 30), maskHigh(63, 64, 65), maskHigh(90, 91)};
    }

    private static long maskLow(int... cells) {
        long result = 0L;
        for (int cell : cells) result |= BitBoard100.lowMask(cell);
        return result;
    }

    private static long maskHigh(int... cells) {
        long result = 0L;
        for (int cell : cells) result |= BitBoard100.highMask(cell);
        return result;
    }
}
