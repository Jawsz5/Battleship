package optimized.probability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import optimized.engine.OptimizedGame;
import optimized.strategies.OptimizedStrategy;

/** Fixed-worker independent-game runner; workers share only immutable geometry and a read-only corpus. */
public final class ParallelSimulationRunner implements AutoCloseable {
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    private final int workerCount;
    private final ExecutorService executor;
    private final SimulationWorker[] workers;

    public ParallelSimulationRunner(int workerCount) {
        if (workerCount <= 0) throw new IllegalArgumentException("workerCount must be positive");
        this.workerCount = workerCount;
        this.workers = new SimulationWorker[workerCount];
        for (int worker = 0; worker < workerCount; worker++) workers[worker] = new SimulationWorker();
        ThreadFactory factory = new ThreadFactory() {
            private int next;

            @Override
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "battleship-sim-" + next++);
                thread.setDaemon(true);
                return thread;
            }
        };
        executor = Executors.newFixedThreadPool(workerCount, factory);
    }

    public int workerCount() { return workerCount; }

    /** Runs worker-local randomly generated fleets using deterministic worker-index seed derivation. */
    public SimulationResult runRandom(long games, long masterSeed) {
        if (games < 0) throw new IllegalArgumentException("games must be non-negative");
        List<Future<WorkerStats>> futures = new ArrayList<>(workerCount);
        for (int worker = 0; worker < workerCount; worker++) {
            long count = partitionCount(games, worker);
            long seed = mix64(masterSeed + GOLDEN_GAMMA * worker);
            SimulationWorker local = workers[worker];
            futures.add(executor.submit(() -> local.runRandom(count, seed)));
        }
        return reduce(futures);
    }

    /** Runs the exact read-only corpus; each worker receives one contiguous static partition. */
    public SimulationResult runCorpus(Phase2ProbabilityBenchmark.FleetCorpus corpus) {
        List<Future<WorkerStats>> futures = new ArrayList<>(workerCount);
        for (int worker = 0; worker < workerCount; worker++) {
            int from = partitionStart(corpus.games(), worker);
            int to = from + (int) partitionCount(corpus.games(), worker);
            SimulationWorker local = workers[worker];
            futures.add(executor.submit(() -> local.runCorpus(corpus, from, to)));
        }
        return reduce(futures);
    }

    private long partitionCount(long games, int worker) {
        long base = games / workerCount;
        return base + (worker < games % workerCount ? 1L : 0L);
    }

    private int partitionStart(int games, int worker) {
        int base = games / workerCount;
        int remainder = games % workerCount;
        return worker * base + Math.min(worker, remainder);
    }

    private static SimulationResult reduce(List<Future<WorkerStats>> futures) {
        WorkerStats total = new WorkerStats();
        for (Future<WorkerStats> future : futures) {
            try {
                total.merge(future.get());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for workers", exception);
            } catch (Exception exception) {
                throw new IllegalStateException("Simulation worker failed", exception);
            }
        }
        return total.snapshot();
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    /** Immutable aggregate result; histogram index is the exact number of turns. */
    public record SimulationResult(long games, long totalTurns, int minTurns, int maxTurns,
                                   long[] turnHistogram) {
        public SimulationResult {
            turnHistogram = turnHistogram.clone();
        }

        public double meanTurns() { return games == 0 ? 0.0 : (double) totalTurns / games; }

        public double medianTurns() {
            if (games == 0) return 0.0;
            long leftRank = (games - 1) / 2;
            long rightRank = games / 2;
            int left = percentile(leftRank);
            int right = percentile(rightRank);
            return (left + right) / 2.0;
        }

        private int percentile(long rank) {
            long cumulative = 0L;
            for (int turns = 0; turns < turnHistogram.length; turns++) {
                cumulative += turnHistogram[turns];
                if (cumulative > rank) return turns;
            }
            throw new IllegalStateException("Histogram does not contain requested rank");
        }

        public boolean sameStatistics(SimulationResult other) {
            return games == other.games && totalTurns == other.totalTurns
                    && minTurns == other.minTurns && maxTurns == other.maxTurns
                    && Arrays.equals(turnHistogram, other.turnHistogram);
        }
    }

    private static final class SimulationWorker {
        private final OptimizedGame game = new OptimizedGame();
        private final OptimizedStrategy strategy = new BucketSelectionBoostedNoHuntStrategy();
        private final long[] fleetLo = new long[OptimizedGame.SHIP_COUNT];
        private final long[] fleetHi = new long[OptimizedGame.SHIP_COUNT];
        private final WorkerStats stats = new WorkerStats();
        private SplittableRandom random;

        WorkerStats runRandom(long games, long seed) {
            stats.clear();
            random = new SplittableRandom(seed);
            for (long index = 0; index < games; index++) {
                game.resetRandom(random);
                strategy.reset(random);
                stats.record(simulate());
            }
            return stats;
        }

        WorkerStats runCorpus(Phase2ProbabilityBenchmark.FleetCorpus corpus, int from, int to) {
            stats.clear();
            for (int gameIndex = from; gameIndex < to; gameIndex++) {
                System.arraycopy(corpus.low(), gameIndex * OptimizedGame.SHIP_COUNT,
                        fleetLo, 0, OptimizedGame.SHIP_COUNT);
                System.arraycopy(corpus.high(), gameIndex * OptimizedGame.SHIP_COUNT,
                        fleetHi, 0, OptimizedGame.SHIP_COUNT);
                game.resetWithFleet(fleetLo, fleetHi);
                strategy.reset(randomForCorpus());
                stats.record(simulate());
            }
            return stats;
        }

        private int simulate() {
            int turns = 0;
            while (!game.isComplete()) {
                int shot = strategy.chooseShot();
                int result = game.shoot(shot);
                strategy.trackShot(shot, result, game.lastSunkLo(), game.lastSunkHi());
                turns++;
            }
            return turns;
        }

        private SplittableRandom randomForCorpus() {
            if (random == null) random = new SplittableRandom(0L);
            return random;
        }
    }

    private static final class WorkerStats {
        private final long[] histogram = new long[101];
        private long games;
        private long turns;
        private int min = 101;
        private int max;

        void clear() {
            Arrays.fill(histogram, 0L);
            games = turns = 0L;
            min = 101;
            max = 0;
        }

        void record(int gameTurns) {
            if (gameTurns < 0 || gameTurns >= histogram.length) {
                throw new IllegalStateException("Unexpected game length: " + gameTurns);
            }
            games++;
            turns += gameTurns;
            histogram[gameTurns]++;
            min = Math.min(min, gameTurns);
            max = Math.max(max, gameTurns);
        }

        void merge(WorkerStats other) {
            games += other.games;
            turns += other.turns;
            min = Math.min(min, other.min);
            max = Math.max(max, other.max);
            for (int turns = 0; turns < histogram.length; turns++) histogram[turns] += other.histogram[turns];
        }

        SimulationResult snapshot() {
            return new SimulationResult(games, turns, games == 0 ? 0 : min, max, histogram);
        }
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
