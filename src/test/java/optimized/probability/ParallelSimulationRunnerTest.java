package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParallelSimulationRunnerTest {
    private static final long SEED = 0x5A1E_2026L;

    @Test
    void exactCorpusStatisticsAreIndependentOfWorkerCount() {
        Phase2ProbabilityBenchmark.FleetCorpus corpus = Phase2ProbabilityBenchmark.generateCorpus(10_000, SEED);
        ParallelSimulationRunner.SimulationResult one = runCorpus(corpus, 1);
        ParallelSimulationRunner.SimulationResult two = runCorpus(corpus, 2);
        ParallelSimulationRunner.SimulationResult four = runCorpus(corpus, 4);
        assertSame(one, two);
        assertSame(one, four);
    }

    @Test
    void corpusHistogramMatchesExistingSingleThreadRunnerStatistics() {
        Phase2ProbabilityBenchmark.FleetCorpus corpus = Phase2ProbabilityBenchmark.generateCorpus(10_000, SEED);
        Phase2ProbabilityBenchmark.Result expected = Phase2ProbabilityBenchmark.runOptimized(corpus,
                new BucketSelectionBoostedNoHuntStrategy());
        ParallelSimulationRunner.SimulationResult actual = runCorpus(corpus, 1);
        assertEquals(expected.games(), actual.games());
        assertEquals(expected.totalTurns(), actual.totalTurns());
        assertEquals(expected.mean(), actual.meanTurns());
        assertEquals(expected.median(), actual.medianTurns());
        assertEquals(expected.min(), actual.minTurns());
        assertEquals(expected.max(), actual.maxTurns());
    }

    @Test
    void repeatedMulticoreCorpusStressIsStable() {
        Phase2ProbabilityBenchmark.FleetCorpus corpus = Phase2ProbabilityBenchmark.generateCorpus(2_000, SEED);
        ParallelSimulationRunner.SimulationResult expected = runCorpus(corpus, 1);
        try (ParallelSimulationRunner runner = new ParallelSimulationRunner(4)) {
            for (int attempt = 0; attempt < 10; attempt++) {
                ParallelSimulationRunner.SimulationResult actual = runner.runCorpus(corpus);
                assertSame(expected, actual);
            }
        }
    }

    @Test
    void sameMasterSeedAndWorkerCountReproduceRandomAggregate() {
        try (ParallelSimulationRunner runner = new ParallelSimulationRunner(4)) {
            ParallelSimulationRunner.SimulationResult first = runner.runRandom(20_000, SEED);
            ParallelSimulationRunner.SimulationResult second = runner.runRandom(20_000, SEED);
            assertSame(first, second);
        }
    }

    private static ParallelSimulationRunner.SimulationResult runCorpus(
            Phase2ProbabilityBenchmark.FleetCorpus corpus, int workers) {
        try (ParallelSimulationRunner runner = new ParallelSimulationRunner(workers)) {
            return runner.runCorpus(corpus);
        }
    }

    private static void assertSame(ParallelSimulationRunner.SimulationResult expected,
                                   ParallelSimulationRunner.SimulationResult actual) {
        assertTrue(expected.sameStatistics(actual));
        assertEquals(expected.meanTurns(), actual.meanTurns());
        assertEquals(expected.medianTurns(), actual.medianTurns());
    }
}
