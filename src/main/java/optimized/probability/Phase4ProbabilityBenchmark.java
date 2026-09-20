package optimized.probability;

import java.util.SplittableRandom;
import optimized.engine.BitBoard100;
import optimized.engine.ShotResult;
import optimized.strategies.OptimizedStrategy;

/** Single-thread D/E benchmark for scan, score-bucket, and tournament selection. */
public final class Phase4ProbabilityBenchmark {
    private static volatile long blackhole;

    private Phase4ProbabilityBenchmark() {}

    public static void main(String[] args) {
        int warmupGames = Integer.getInteger("phase4.warmupGames", 5_000);
        int trials = Integer.getInteger("phase4.trials", 3);
        int largeGames = Integer.getInteger("phase4.largeGames", 100_000);
        int millionGames = Integer.getInteger("phase4.millionGames", 1_000_000);

        String only = System.getProperty("phase4.only");
        if (only != null) {
            OptimizedStrategy strategy = strategy(only);
            Phase2ProbabilityBenchmark.FleetCorpus warmup =
                    Phase2ProbabilityBenchmark.generateCorpus(warmupGames, 0xA11CE4L);
            Phase2ProbabilityBenchmark.runOptimized(warmup, strategy);
            Phase2ProbabilityBenchmark.FleetCorpus corpus = Phase2ProbabilityBenchmark.generateCorpus(
                    Integer.getInteger("phase4.profileGames", 500_000), 0xE04L);
            System.out.println("scale,implementation,games,time_ms,games_per_sec,total_turns,mean,median,min,max");
            runScale(corpus, trials, strategy, "focused", label(only));
            return;
        }

        Phase2ProbabilityBenchmark.FleetCorpus warmup =
                Phase2ProbabilityBenchmark.generateCorpus(warmupGames, 0xA11CE4L);
        warm(warmup);

        System.out.printf("Phase 4 single-thread benchmark: warmup=%d, trials=%d%n", warmupGames, trials);
        System.out.println("scale,implementation,games,time_ms,games_per_sec,total_turns,mean,median,min,max");
        runAll(Phase2ProbabilityBenchmark.generateCorpus(10_000, 0x10_004L), trials, "10k");
        runAll(Phase2ProbabilityBenchmark.generateCorpus(largeGames, 0x100_004L), trials, "100k");
        runAll(Phase2ProbabilityBenchmark.generateCorpus(millionGames, 0x1_000_004L), trials, "1m");
        benchmarkOperations();
    }

    private static void warm(Phase2ProbabilityBenchmark.FleetCorpus corpus) {
        Phase2ProbabilityBenchmark.runOptimized(corpus, new IncrementalBonusBoostedNoHuntStrategy());
        Phase2ProbabilityBenchmark.runOptimized(corpus, new BucketSelectionBoostedNoHuntStrategy());
        Phase2ProbabilityBenchmark.runOptimized(corpus, new TournamentSelectionBoostedNoHuntStrategy());
    }

    private static void runAll(Phase2ProbabilityBenchmark.FleetCorpus corpus, int trials, String scale) {
        runScale(corpus, trials, new IncrementalBonusBoostedNoHuntStrategy(), scale,
                "D-phase3-linear-scan");
        runScale(corpus, trials, new BucketSelectionBoostedNoHuntStrategy(), scale,
                "E1-phase4-score-buckets");
        runScale(corpus, trials, new TournamentSelectionBoostedNoHuntStrategy(), scale,
                "E2-phase4-tournament");
    }

    private static void runScale(Phase2ProbabilityBenchmark.FleetCorpus corpus, int trials,
                                 OptimizedStrategy strategy, String scale, String name) {
        Phase2ProbabilityBenchmark.Result[] results = new Phase2ProbabilityBenchmark.Result[trials];
        for (int trial = 0; trial < trials; trial++) {
            results[trial] = Phase2ProbabilityBenchmark.runOptimized(corpus, strategy);
        }
        Phase2ProbabilityBenchmark.print(scale, name, Phase2ProbabilityBenchmark.medianTrial(results));
    }

    private static OptimizedStrategy strategy(String name) {
        return switch (name) {
            case "scan" -> new IncrementalBonusBoostedNoHuntStrategy();
            case "bucket" -> new BucketSelectionBoostedNoHuntStrategy();
            case "tournament" -> new TournamentSelectionBoostedNoHuntStrategy();
            default -> throw new IllegalArgumentException("Unknown phase4.only: " + name);
        };
    }

    private static String label(String name) {
        return switch (name) {
            case "scan" -> "D-phase3-linear-scan";
            case "bucket" -> "E1-phase4-score-buckets";
            case "tournament" -> "E2-phase4-tournament";
            default -> name;
        };
    }

    private static void benchmarkOperations() {
        int operations = Integer.getInteger("phase4.microOperations", 200_000);
        benchmarkArchitecture("scan", new IncrementalBonusBoostedNoHuntStrategy(), operations);
        benchmarkArchitecture("bucket", new BucketSelectionBoostedNoHuntStrategy(), operations);
        benchmarkArchitecture("tournament", new TournamentSelectionBoostedNoHuntStrategy(), operations);
    }

    private static void benchmarkArchitecture(String name, OptimizedStrategy strategy, int operations) {
        SplittableRandom random = new SplittableRandom(17);
        measure(name + "_reset", operations, () -> strategy.reset(random));
        measure(name + "_reset_plus_central_miss_update", operations, () -> {
            strategy.reset(random);
            strategy.trackShot(44, ShotResult.MISS, 0L, 0L);
        });
        measure(name + "_reset_plus_isolated_hit_update", operations, () -> {
            strategy.reset(random);
            strategy.trackShot(44, ShotResult.HIT, 0L, 0L);
        });
        measure(name + "_reset_plus_three_hit_updates", operations, () -> {
            strategy.reset(random);
            strategy.trackShot(44, ShotResult.HIT, 0L, 0L);
            strategy.trackShot(45, ShotResult.HIT, 0L, 0L);
            strategy.trackShot(46, ShotResult.HIT, 0L, 0L);
        });
        long sinkLo = BitBoard100.lowMask(42) | BitBoard100.lowMask(43)
                | BitBoard100.lowMask(44) | BitBoard100.lowMask(45) | BitBoard100.lowMask(46);
        measure(name + "_reset_plus_length5_sink_update", operations, () -> {
            strategy.reset(random);
            strategy.trackShot(42, ShotResult.HIT, 0L, 0L);
            strategy.trackShot(43, ShotResult.HIT, 0L, 0L);
            strategy.trackShot(44, ShotResult.HIT, 0L, 0L);
            strategy.trackShot(45, ShotResult.HIT, 0L, 0L);
            strategy.trackShot(46, ShotResult.sunk(5), sinkLo, 0L);
        });
        strategy.reset(random);
        measure(name + "_candidate_lookup", operations, () -> blackhole ^= strategy.chooseShot());
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
