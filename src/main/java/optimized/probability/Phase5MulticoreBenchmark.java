package optimized.probability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Measures fixed-worker independent-game scaling with worker-local Phase 4 state. */
public final class Phase5MulticoreBenchmark {
    private static final long MASTER_SEED = 0x5A1E_2026_1234_5678L;

    private Phase5MulticoreBenchmark() {}

    public static void main(String[] args) {
        int logicalProcessors = Runtime.getRuntime().availableProcessors();
        int warmupGames = Integer.getInteger("phase5.warmupGames", 200_000);
        int corpusGames = Integer.getInteger("phase5.corpusGames", 1_000_000);
        int randomGames = Integer.getInteger("phase5.randomGames", 5_000_000);
        int trials = Integer.getInteger("phase5.trials", 3);
        int[] workerCounts = workerCounts(logicalProcessors, System.getProperty("phase5.workers"));

        System.out.printf("Phase 5 fixed-worker benchmark: logical_processors=%d, warmup=%d, trials=%d%n",
                logicalProcessors, warmupGames, trials);
        System.out.println("mode,workers,games,time_ms,games_per_sec,total_turns,mean,median,min,max,speedup,efficiency");

        Phase2ProbabilityBenchmark.FleetCorpus corpus =
                Phase2ProbabilityBenchmark.generateCorpus(corpusGames, MASTER_SEED);
        List<Measured> corpusResults = measureAll("corpus", corpusGames, trials, warmupGames, workerCounts,
                runner -> runner.runCorpus(corpus));
        printAll(corpusResults);

        List<Measured> randomResults = measureAll("worker-local-random", randomGames, trials, warmupGames,
                workerCounts, runner -> runner.runRandom(randomGames, MASTER_SEED));
        printAll(randomResults);
    }

    private static List<Measured> measureAll(String mode, long games, int trials, int warmupGames,
                                             int[] workerCounts, Run run) {
        List<Measured> measured = new ArrayList<>();
        for (int workers : workerCounts) {
            try (ParallelSimulationRunner runner = new ParallelSimulationRunner(workers)) {
                runner.runRandom(warmupGames, MASTER_SEED ^ workers);
                Trial[] results = new Trial[trials];
                for (int trial = 0; trial < trials; trial++) {
                    long start = System.nanoTime();
                    ParallelSimulationRunner.SimulationResult statistics = run.apply(runner);
                    results[trial] = new Trial(System.nanoTime() - start, statistics);
                }
                Arrays.sort(results, Comparator.comparingLong(Trial::elapsedNanos));
                measured.add(new Measured(mode, workers, results[trials / 2]));
            }
        }
        return measured;
    }

    private static void printAll(List<Measured> measured) {
        double baseline = measured.getFirst().throughput();
        for (Measured result : measured) {
            ParallelSimulationRunner.SimulationResult stats = result.trial.statistics;
            double speedup = result.throughput() / baseline;
            System.out.printf("%s,%d,%d,%.3f,%.1f,%d,%.5f,%.1f,%d,%d,%.4f,%.4f%n",
                    result.mode, result.workers, stats.games(), result.trial.elapsedNanos / 1_000_000.0,
                    result.throughput(), stats.totalTurns(), stats.meanTurns(), stats.medianTurns(),
                    stats.minTurns(), stats.maxTurns(), speedup, speedup / result.workers);
        }
    }

    private static int[] workerCounts(int logicalProcessors, String requested) {
        if (requested != null) {
            return Arrays.stream(requested.split(",")).mapToInt(Integer::parseInt).toArray();
        }
        int limit = Math.max(1, logicalProcessors);
        int[] candidates = new int[16];
        int count = 0;
        for (int workers = 1; workers < limit; workers <<= 1) candidates[count++] = workers;
        if (count == 0 || candidates[count - 1] != limit) candidates[count++] = limit;
        return Arrays.copyOf(candidates, count);
    }

    @FunctionalInterface
    private interface Run {
        ParallelSimulationRunner.SimulationResult apply(ParallelSimulationRunner runner);
    }

    private record Trial(long elapsedNanos, ParallelSimulationRunner.SimulationResult statistics) {}

    private record Measured(String mode, int workers, Trial trial) {
        double throughput() { return trial.statistics.games() * 1_000_000_000.0 / trial.elapsedNanos; }
    }
}
