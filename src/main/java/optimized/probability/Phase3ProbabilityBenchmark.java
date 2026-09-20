package optimized.probability;

import java.util.SplittableRandom;
import optimized.engine.BitBoard100;
import optimized.engine.ShotResult;

/** Single-thread A/B/C/D benchmark plus Phase 3 dynamic-update characterization. */
public final class Phase3ProbabilityBenchmark {
    private static volatile long blackhole;

    public static void main(String[] args) throws Exception {
        int warmupGames = Integer.getInteger("phase3.warmupGames", 2_000);
        int trials = Integer.getInteger("phase3.trials", 3);
        int largeGames = Integer.getInteger("phase3.largeGames", 100_000);
        int millionGames = Integer.getInteger("phase3.millionGames", 1_000_000);

        if (Boolean.getBoolean("phase3.onlyD")) {
            runFocusedD(warmupGames, trials, Integer.getInteger("phase3.profileGames", 500_000));
            return;
        }

        Phase2ProbabilityBenchmark.FleetCorpus warmup =
                Phase2ProbabilityBenchmark.generateCorpus(warmupGames, 0xA11CE5L);
        Phase2ProbabilityBenchmark.runLegacy(warmup);
        Phase2ProbabilityBenchmark.runOptimized(warmup, new RecomputeBoostedNoHuntStrategy());
        Phase2ProbabilityBenchmark.runOptimized(warmup, new IncrementalBoostedNoHuntStrategy());
        Phase2ProbabilityBenchmark.runOptimized(warmup, new IncrementalBonusBoostedNoHuntStrategy());

        System.out.printf("Phase 3 single-thread benchmark: warmup=%d, trials=%d%n", warmupGames, trials);
        System.out.println("scale,implementation,games,time_ms,games_per_sec,total_turns,mean,median,min,max");
        runComparable(Phase2ProbabilityBenchmark.generateCorpus(10_000, 0x10_000L), trials, "10k");
        runComparable(Phase2ProbabilityBenchmark.generateCorpus(largeGames, 0x100_000L), trials, "100k");

        Phase2ProbabilityBenchmark.FleetCorpus million =
                Phase2ProbabilityBenchmark.generateCorpus(millionGames, 0x1_000_000L);
        runOptimizedScale(million, trials, new IncrementalBoostedNoHuntStrategy(),
                "1m", "C-phase2-incremental");
        runOptimizedScale(million, trials, new IncrementalBonusBoostedNoHuntStrategy(),
                "1m", "D-phase3-incremental-bonus");
        benchmarkDynamicOperations();
    }

    private static void runComparable(Phase2ProbabilityBenchmark.FleetCorpus corpus,
                                      int trials, String scale) throws Exception {
        Phase2ProbabilityBenchmark.Result[] a = new Phase2ProbabilityBenchmark.Result[trials];
        Phase2ProbabilityBenchmark.Result[] b = new Phase2ProbabilityBenchmark.Result[trials];
        Phase2ProbabilityBenchmark.Result[] c = new Phase2ProbabilityBenchmark.Result[trials];
        Phase2ProbabilityBenchmark.Result[] d = new Phase2ProbabilityBenchmark.Result[trials];
        for (int trial = 0; trial < trials; trial++) {
            a[trial] = Phase2ProbabilityBenchmark.runLegacy(corpus);
            b[trial] = Phase2ProbabilityBenchmark.runOptimized(corpus,
                    new RecomputeBoostedNoHuntStrategy());
            c[trial] = Phase2ProbabilityBenchmark.runOptimized(corpus,
                    new IncrementalBoostedNoHuntStrategy());
            d[trial] = Phase2ProbabilityBenchmark.runOptimized(corpus,
                    new IncrementalBonusBoostedNoHuntStrategy());
        }
        Phase2ProbabilityBenchmark.print(scale, "A-legacy", Phase2ProbabilityBenchmark.medianTrial(a));
        Phase2ProbabilityBenchmark.print(scale, "B-optimized-recompute", Phase2ProbabilityBenchmark.medianTrial(b));
        Phase2ProbabilityBenchmark.print(scale, "C-phase2-incremental", Phase2ProbabilityBenchmark.medianTrial(c));
        Phase2ProbabilityBenchmark.print(scale, "D-phase3-incremental-bonus", Phase2ProbabilityBenchmark.medianTrial(d));
    }

    private static void runOptimizedScale(Phase2ProbabilityBenchmark.FleetCorpus corpus, int trials,
                                          optimized.strategies.OptimizedStrategy strategy,
                                          String scale, String name) {
        Phase2ProbabilityBenchmark.Result[] results = new Phase2ProbabilityBenchmark.Result[trials];
        for (int trial = 0; trial < trials; trial++) {
            results[trial] = Phase2ProbabilityBenchmark.runOptimized(corpus, strategy);
        }
        Phase2ProbabilityBenchmark.print(scale, name, Phase2ProbabilityBenchmark.medianTrial(results));
    }

    private static void runFocusedD(int warmupGames, int trials, int games) {
        Phase2ProbabilityBenchmark.FleetCorpus warmup =
                Phase2ProbabilityBenchmark.generateCorpus(warmupGames, 0xD00DL);
        Phase2ProbabilityBenchmark.runOptimized(warmup, new IncrementalBonusBoostedNoHuntStrategy());
        Phase2ProbabilityBenchmark.FleetCorpus corpus =
                Phase2ProbabilityBenchmark.generateCorpus(games, 0xD03L);
        System.out.println("scale,implementation,games,time_ms,games_per_sec,total_turns,mean,median,min,max");
        runOptimizedScale(corpus, trials, new IncrementalBonusBoostedNoHuntStrategy(),
                "focused", "D-phase3-incremental-bonus");
    }

    private static void benchmarkDynamicOperations() {
        int operations = Integer.getInteger("phase3.microOperations", 200_000);
        SplittableRandom random = new SplittableRandom(17);
        IncrementalBonusBoostedNoHuntStrategy strategy = new IncrementalBonusBoostedNoHuntStrategy();

        measure("bonus_reset", operations, () -> strategy.reset(random));
        measure("reset_plus_miss_full_update", operations, () -> {
            strategy.reset(random);
            strategy.trackShot(44, ShotResult.MISS, 0L, 0L);
        });
        measure("reset_plus_isolated_hit", operations, () -> {
            strategy.reset(random);
            strategy.trackShot(44, ShotResult.HIT, 0L, 0L);
        });
        measure("reset_plus_two_aligned_hits", operations, () -> {
            strategy.reset(random);
            strategy.trackShot(44, ShotResult.HIT, 0L, 0L);
            strategy.trackShot(45, ShotResult.HIT, 0L, 0L);
        });
        measure("reset_plus_three_aligned_hits", operations, () -> {
            strategy.reset(random);
            strategy.trackShot(44, ShotResult.HIT, 0L, 0L);
            strategy.trackShot(45, ShotResult.HIT, 0L, 0L);
            strategy.trackShot(46, ShotResult.HIT, 0L, 0L);
        });

        measureSink(strategy, random, operations, 2, 44, 45);
        measureSink(strategy, random, operations, 3, 44, 45, 46);
        measureSink(strategy, random, operations, 5, 42, 43, 44, 45, 46);

        strategy.reset(random);
        measure("candidate_selection", operations, () -> blackhole ^= strategy.chooseShot());
    }

    private static void measureSink(IncrementalBonusBoostedNoHuntStrategy strategy,
                                    SplittableRandom random, int operations,
                                    int length, int... cells) {
        long lo = 0L;
        long hi = 0L;
        for (int cell : cells) {
            lo |= BitBoard100.lowMask(cell);
            hi |= BitBoard100.highMask(cell);
        }
        long sunkLo = lo;
        long sunkHi = hi;
        measure("reset_plus_sunk_length_" + length, operations, () -> {
            strategy.reset(random);
            for (int index = 0; index < cells.length - 1; index++) {
                strategy.trackShot(cells[index], ShotResult.HIT, 0L, 0L);
            }
            strategy.trackShot(cells[cells.length - 1], ShotResult.sunk(length), sunkLo, sunkHi);
        });
    }

    private static void measure(String name, int operations, Runnable operation) {
        long start = System.nanoTime();
        for (int index = 0; index < operations; index++) operation.run();
        long elapsed = System.nanoTime() - start;
        blackhole ^= elapsed;
        System.out.printf("micro,%s,%d,%.3f,%.3f%n", name, operations,
                elapsed / 1_000_000.0, (double) elapsed / operations);
    }
}
