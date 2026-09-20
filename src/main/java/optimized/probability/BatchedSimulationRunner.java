package optimized.probability;

import java.util.Arrays;
import java.util.SplittableRandom;
import optimized.engine.OptimizedGame;
import optimized.strategies.OptimizedStrategy;

/** Single-worker lane-refilling batch experiment; each lane owns complete Phase 4 state. */
public final class BatchedSimulationRunner {
    private final Lane[] lanes;
    private final int batchWidth;

    public BatchedSimulationRunner(int batchWidth) {
        if (batchWidth <= 0) throw new IllegalArgumentException("batchWidth must be positive");
        this.batchWidth = batchWidth;
        lanes = new Lane[batchWidth];
        for (int lane = 0; lane < batchWidth; lane++) lanes[lane] = new Lane(lane);
    }

    public int batchWidth() { return batchWidth; }

    public BatchResult runCorpus(Phase2ProbabilityBenchmark.FleetCorpus corpus) {
        return runRange(corpus, 0, corpus.games());
    }

    BatchResult runRange(Phase2ProbabilityBenchmark.FleetCorpus corpus, int from, int to) {
        Stats stats = new Stats();
        int next = from;
        int active = 0;
        for (Lane lane : lanes) {
            if (next < to) {
                lane.start(corpus, next++);
                active++;
            } else {
                lane.active = false;
            }
        }
        while (active != 0) {
            for (Lane lane : lanes) {
                if (!lane.active) continue;
                lane.step();
                if (lane.game.isComplete()) {
                    stats.record(lane.turns);
                    if (next < to) lane.start(corpus, next++);
                    else {
                        lane.active = false;
                        active--;
                    }
                }
            }
        }
        return stats.snapshot();
    }

    /** Captures exact selected-shot sequences for a small indexed-corpus parity sample. */
    public int[][] sampleShotSequences(Phase2ProbabilityBenchmark.FleetCorpus corpus, int sampleGames) {
        if (sampleGames < 0 || sampleGames > corpus.games()) throw new IllegalArgumentException("sampleGames");
        int[][] sequences = new int[sampleGames][100];
        int[] lengths = new int[sampleGames];
        int next = 0;
        int active = 0;
        for (Lane lane : lanes) {
            if (next < sampleGames) {
                lane.start(corpus, next++);
                active++;
            } else lane.active = false;
        }
        while (active != 0) {
            for (Lane lane : lanes) {
                if (!lane.active) continue;
                int shot = lane.step();
                sequences[lane.gameIndex][lengths[lane.gameIndex]++] = shot;
                if (lane.game.isComplete()) {
                    if (next < sampleGames) lane.start(corpus, next++);
                    else {
                        lane.active = false;
                        active--;
                    }
                }
            }
        }
        for (int game = 0; game < sampleGames; game++) {
            sequences[game] = Arrays.copyOf(sequences[game], lengths[game]);
        }
        return sequences;
    }

    public record BatchResult(long games, long totalTurns, int minTurns, int maxTurns,
                              long[] turnHistogram) {
        public BatchResult {
            turnHistogram = turnHistogram.clone();
        }

        public double meanTurns() { return games == 0 ? 0.0 : (double) totalTurns / games; }

        public double medianTurns() {
            if (games == 0) return 0.0;
            long leftRank = (games - 1) / 2;
            long rightRank = games / 2;
            return (percentile(leftRank) + percentile(rightRank)) / 2.0;
        }

        private int percentile(long rank) {
            long cumulative = 0;
            for (int turns = 0; turns < turnHistogram.length; turns++) {
                cumulative += turnHistogram[turns];
                if (cumulative > rank) return turns;
            }
            throw new IllegalStateException("Histogram rank outside result");
        }
    }

    private static final class Lane {
        private final OptimizedGame game = new OptimizedGame();
        private final OptimizedStrategy strategy = new BucketSelectionBoostedNoHuntStrategy();
        private final long[] fleetLo = new long[OptimizedGame.SHIP_COUNT];
        private final long[] fleetHi = new long[OptimizedGame.SHIP_COUNT];
        private final SplittableRandom resetRandom;
        private int gameIndex;
        private int turns;
        private boolean active;

        Lane(int lane) { resetRandom = new SplittableRandom(0x6A09E667F3BCC909L + lane); }

        void start(Phase2ProbabilityBenchmark.FleetCorpus corpus, int index) {
            gameIndex = index;
            turns = 0;
            System.arraycopy(corpus.low(), index * OptimizedGame.SHIP_COUNT,
                    fleetLo, 0, OptimizedGame.SHIP_COUNT);
            System.arraycopy(corpus.high(), index * OptimizedGame.SHIP_COUNT,
                    fleetHi, 0, OptimizedGame.SHIP_COUNT);
            game.resetWithFleet(fleetLo, fleetHi);
            strategy.reset(resetRandom);
            active = true;
        }

        int step() {
            int shot = strategy.chooseShot();
            int result = game.shoot(shot);
            strategy.trackShot(shot, result, game.lastSunkLo(), game.lastSunkHi());
            turns++;
            return shot;
        }
    }

    private static final class Stats {
        private final long[] histogram = new long[101];
        private long games;
        private long turns;
        private int min = 101;
        private int max;

        void record(int gameTurns) {
            games++;
            turns += gameTurns;
            histogram[gameTurns]++;
            min = Math.min(min, gameTurns);
            max = Math.max(max, gameTurns);
        }

        BatchResult snapshot() {
            return new BatchResult(games, turns, games == 0 ? 0 : min, max, histogram);
        }
    }
}
