package optimized.probability;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Final SIMD/batch experiments; the Phase 5 scalar runner remains the reference. */
public final class Phase6SimdBatchBenchmark {
    private static final long SEED = 0x6A09E667F3BCC909L;

    private Phase6SimdBatchBenchmark() {}

    public static void main(String[] args) {
        int games = Integer.getInteger("phase6.games", 1_000_000);
        int trials = Integer.getInteger("phase6.trials", 3);
        int[] widths = widths(System.getProperty("phase6.widths", "1,2,4,8,16"));
        Phase2ProbabilityBenchmark.FleetCorpus corpus =
                Phase2ProbabilityBenchmark.generateCorpus(games, SEED);

        System.out.printf("Vector API: available=true,module=jdk.incubator.vector,species=%d%n",
                VectorApiExperiment.vectorWidth());
        benchmarkVectorArithmetic();
        System.out.println("mode,width,games,time_ms,games_per_sec,total_turns,mean,median,min,max,speedup_vs_width1");
        List<Measured> measured = new java.util.ArrayList<>();
        boolean singleWorkerCurve = Boolean.parseBoolean(System.getProperty("phase6.singleWorker", "true"));
        if (singleWorkerCurve) for (int width : widths) measured.add(measure(corpus, width, trials));
        double baseline = measured.stream().filter(value -> value.width == 1).findFirst()
                .map(Measured::throughput).orElse(0.0);
        for (Measured result : measured) {
            BatchedSimulationRunner.BatchResult stats = result.statistics;
            System.out.printf("batched-scalar,%d,%d,%.3f,%.1f,%d,%.5f,%.1f,%d,%d,%.4f%n",
                    result.width, stats.games(), result.elapsedNanos / 1_000_000.0,
                    result.throughput(), stats.totalTurns(), stats.meanTurns(), stats.medianTurns(),
                    stats.minTurns(), stats.maxTurns(), baseline == 0.0 ? 0.0 : result.throughput() / baseline);
        }
        int parallelWorkers = Integer.getInteger("phase6.parallelWorkers", 14);
        System.out.printf("parallel batch workers=%d%n", parallelWorkers);
        try (ParallelSimulationRunner scalar = new ParallelSimulationRunner(parallelWorkers)) {
            for (int trial = 0; trial < trials; trial++) {
                long start = System.nanoTime();
                ParallelSimulationRunner.SimulationResult result = scalar.runCorpus(corpus);
                long elapsed = System.nanoTime() - start;
                System.out.printf("scalar-parallel,%d,%d,%.3f,%.1f,%d,%.5f,%.1f,%d,%d%n",
                        parallelWorkers, result.games(), elapsed / 1_000_000.0,
                        result.games() * 1_000_000_000.0 / elapsed, result.totalTurns(),
                        result.meanTurns(), result.medianTurns(), result.minTurns(), result.maxTurns());
            }
        }
        for (int width : widths) {
            Measured result = measureParallel(corpus, width, parallelWorkers, trials);
            System.out.printf("batched-parallel,%d,%d,%.3f,%.1f,%d,%.5f,%.1f,%d,%d%n",
                    width, result.statistics.games(), result.elapsedNanos / 1_000_000.0,
                    result.throughput(), result.statistics.totalTurns(), result.statistics.meanTurns(),
                    result.statistics.medianTurns(), result.statistics.minTurns(), result.statistics.maxTurns());
        }
    }

    private static Measured measure(Phase2ProbabilityBenchmark.FleetCorpus corpus, int width, int trials) {
        BatchedSimulationRunner runner = new BatchedSimulationRunner(width);
        runner.runCorpus(corpus);
        Trial[] trialsRun = new Trial[trials];
        for (int trial = 0; trial < trials; trial++) {
            long start = System.nanoTime();
            BatchedSimulationRunner.BatchResult statistics = runner.runCorpus(corpus);
            trialsRun[trial] = new Trial(System.nanoTime() - start, statistics);
        }
        Arrays.sort(trialsRun, Comparator.comparingLong(Trial::elapsedNanos));
        Trial selected = trialsRun[trials / 2];
        return new Measured(width, selected.elapsedNanos, selected.statistics);
    }

    private static Measured measureParallel(Phase2ProbabilityBenchmark.FleetCorpus corpus, int width,
                                            int workers, int trials) {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        BatchedSimulationRunner[] runners = new BatchedSimulationRunner[workers];
        for (int worker = 0; worker < workers; worker++) runners[worker] = new BatchedSimulationRunner(width);
        Trial[] trialsRun = new Trial[trials];
        for (int trial = 0; trial < trials; trial++) {
            long start = System.nanoTime();
            List<Future<BatchedSimulationRunner.BatchResult>> futures = new ArrayList<>(workers);
            for (int worker = 0; worker < workers; worker++) {
                int from = worker * corpus.games() / workers;
                int to = (worker + 1) * corpus.games() / workers;
                BatchedSimulationRunner runner = runners[worker];
                futures.add(executor.submit(() -> runner.runRange(corpus, from, to)));
            }
            BatchedSimulationRunner.BatchResult statistics = reduce(futures);
            trialsRun[trial] = new Trial(System.nanoTime() - start, statistics);
        }
        executor.shutdown();
        Arrays.sort(trialsRun, Comparator.comparingLong(Trial::elapsedNanos));
        Trial selected = trialsRun[trials / 2];
        return new Measured(width, selected.elapsedNanos, selected.statistics);
    }

    private static BatchedSimulationRunner.BatchResult reduce(
            List<Future<BatchedSimulationRunner.BatchResult>> futures) {
        long games = 0;
        long totalTurns = 0;
        int minTurns = 101;
        int maxTurns = 0;
        long[] histogram = new long[101];
        for (Future<BatchedSimulationRunner.BatchResult> future : futures) {
            try {
                BatchedSimulationRunner.BatchResult result = future.get();
                games += result.games();
                totalTurns += result.totalTurns();
                minTurns = Math.min(minTurns, result.minTurns());
                maxTurns = Math.max(maxTurns, result.maxTurns());
                for (int turns = 0; turns < histogram.length; turns++) {
                    histogram[turns] += result.turnHistogram()[turns];
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted during batch reduction", exception);
            } catch (Exception exception) {
                throw new IllegalStateException("Batch worker failed", exception);
            }
        }
        return new BatchedSimulationRunner.BatchResult(games, totalTurns,
                games == 0 ? 0 : minTurns, maxTurns, histogram);
    }

    private static void benchmarkVectorArithmetic() {
        int operations = Integer.getInteger("phase6.vectorOperations", 5_000_000);
        int[] left = new int[100];
        int[] right = new int[100];
        int[] output = new int[100];
        for (int i = 0; i < 100; i++) { left[i] = i; right[i] = 100 - i; }
        for (int i = 0; i < 100_000; i++) {
            VectorApiExperiment.scalarAddChecksum(left, right, output);
            VectorApiExperiment.vectorAddChecksum(left, right, output);
        }
        long scalarStart = System.nanoTime();
        long scalar = 0;
        for (int i = 0; i < operations; i++) scalar ^= VectorApiExperiment.scalarAddChecksum(left, right, output);
        long scalarNanos = System.nanoTime() - scalarStart;
        long vectorStart = System.nanoTime();
        long vector = 0;
        for (int i = 0; i < operations; i++) vector ^= VectorApiExperiment.vectorAddChecksum(left, right, output);
        long vectorNanos = System.nanoTime() - vectorStart;
        System.out.printf("vector,width=%d,checksum_equal=%s,scalar_ns=%.3f,vector_ns=%.3f,speedup=%.4f%n",
                VectorApiExperiment.vectorWidth(), scalar == vector,
                (double) scalarNanos / operations, (double) vectorNanos / operations,
                (double) scalarNanos / vectorNanos);
    }

    private static int[] widths(String text) {
        return Arrays.stream(text.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    private record Trial(long elapsedNanos, BatchedSimulationRunner.BatchResult statistics) {}

    private record Measured(int width, long elapsedNanos, BatchedSimulationRunner.BatchResult statistics) {
        double throughput() { return statistics.games() * 1_000_000_000.0 / elapsedNanos; }
    }
}
