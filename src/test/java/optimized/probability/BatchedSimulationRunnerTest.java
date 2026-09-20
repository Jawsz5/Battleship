package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import optimized.engine.OptimizedGame;
import optimized.engine.ShotResult;

class BatchedSimulationRunnerTest {
    @Test
    void allBatchWidthsMatchTheIndexedCorpusStatistics() {
        Phase2ProbabilityBenchmark.FleetCorpus corpus =
                Phase2ProbabilityBenchmark.generateCorpus(10_000, 0x6A09E667L);
        BatchedSimulationRunner.BatchResult scalar = new BatchedSimulationRunner(1).runCorpus(corpus);
        for (int width : new int[] {2, 4, 8, 16}) {
            BatchedSimulationRunner.BatchResult actual = new BatchedSimulationRunner(width).runCorpus(corpus);
            assertEquals(scalar.games(), actual.games());
            assertEquals(scalar.totalTurns(), actual.totalTurns());
            assertEquals(scalar.minTurns(), actual.minTurns());
            assertEquals(scalar.maxTurns(), actual.maxTurns());
            assertEquals(scalar.meanTurns(), actual.meanTurns());
            assertEquals(scalar.medianTurns(), actual.medianTurns());
            assertArrayEquals(scalar.turnHistogram(), actual.turnHistogram());
        }
    }

    @Test
    void tenThousandIndexedFleetsHaveExactShotSequenceParity() {
        int games = 10_000;
        Phase2ProbabilityBenchmark.FleetCorpus corpus =
                Phase2ProbabilityBenchmark.generateCorpus(games, 0x6A09E667L);
        int[][] scalar = scalarSequences(corpus);
        int[][] batched = new BatchedSimulationRunner(8).sampleShotSequences(corpus, games);
        assertEquals(scalar.length, batched.length);
        for (int game = 0; game < games; game++) assertArrayEquals(scalar[game], batched[game], "game=" + game);
    }

    @Test
    void vectorProofOfConceptPreservesDenseArithmetic() {
        int[] left = new int[100];
        int[] right = new int[100];
        int[] scalarOutput = new int[100];
        int[] vectorOutput = new int[100];
        for (int index = 0; index < 100; index++) {
            left[index] = index * 3;
            right[index] = 100 - index;
        }
        assertEquals(VectorApiExperiment.scalarAddChecksum(left, right, scalarOutput),
                VectorApiExperiment.vectorAddChecksum(left, right, vectorOutput));
        assertArrayEquals(scalarOutput, vectorOutput);
    }

    private static int[][] scalarSequences(Phase2ProbabilityBenchmark.FleetCorpus corpus) {
        int[][] result = new int[corpus.games()][];
        long[] low = new long[OptimizedGame.SHIP_COUNT];
        long[] high = new long[OptimizedGame.SHIP_COUNT];
        for (int gameIndex = 0; gameIndex < corpus.games(); gameIndex++) {
            System.arraycopy(corpus.low(), gameIndex * OptimizedGame.SHIP_COUNT,
                    low, 0, OptimizedGame.SHIP_COUNT);
            System.arraycopy(corpus.high(), gameIndex * OptimizedGame.SHIP_COUNT,
                    high, 0, OptimizedGame.SHIP_COUNT);
            OptimizedGame game = new OptimizedGame();
            BucketSelectionBoostedNoHuntStrategy strategy = new BucketSelectionBoostedNoHuntStrategy();
            strategy.reset(new SplittableRandom(0));
            game.resetWithFleet(low, high);
            int[] shots = new int[100];
            int turns = 0;
            while (!game.isComplete()) {
                int shot = strategy.chooseShot();
                int encoded = game.shoot(shot);
                strategy.trackShot(shot, encoded, game.lastSunkLo(), game.lastSunkHi());
                shots[turns++] = shot;
            }
            result[gameIndex] = java.util.Arrays.copyOf(shots, turns);
        }
        return result;
    }
}
