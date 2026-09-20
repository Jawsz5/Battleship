package optimized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import legacy.compstrategies.standardstrats.RandomHuntStrat;
import org.junit.jupiter.api.Test;
import optimized.engine.ShotResult;
import optimized.runner.SimulationRunner;
import optimized.runner.SimulationStats;
import optimized.strategies.ParityStrategy;
import optimized.strategies.RandomHuntStrategy;
import optimized.strategies.RandomStrategy;

class StrategySemanticsTest {
    @Test
    void randomNeverRepeatsAndCoversEveryCell() {
        RandomStrategy strategy = new RandomStrategy();
        strategy.reset(new SplittableRandom(1));
        Set<Integer> shots = new HashSet<>();
        for (int index = 0; index < 100; index++) shots.add(strategy.chooseShot());
        assertEquals(100, shots.size());
    }

    @Test
    void parityStartsOnLegacyEvenCheckerboardCandidates() {
        ParityStrategy strategy = new ParityStrategy();
        strategy.reset(new SplittableRandom(2));
        for (int index = 0; index < 20; index++) {
            int shot = strategy.chooseShot();
            assertEquals(0, (shot / 10 + shot % 10) & 1);
            strategy.trackShot(shot, ShotResult.MISS, 0L, 0L);
        }
    }

    @Test
    void huntOrderingMatchesLegacyForControlledHistory() {
        RandomHuntStrat legacy = new RandomHuntStrat(10);
        RandomHuntStrategy optimized = new RandomHuntStrategy();
        optimized.reset(new SplittableRandom(3));

        legacy.trackShot(true, 0, 44, null);
        optimized.trackShot(44, ShotResult.HIT, 0L, 0L);
        int legacyFirst = legacy.selectShot();
        int optimizedFirst = optimized.chooseShot();
        assertEquals(legacyFirst, optimizedFirst);
        assertEquals(34, optimizedFirst);

        legacy.trackShot(false, 0, legacyFirst, null);
        optimized.trackShot(optimizedFirst, ShotResult.MISS, 0L, 0L);
        assertEquals(legacy.selectShot(), optimized.chooseShot());
    }

    @Test
    void reusableRunnerCompletesDeterministicBatch() {
        SimulationRunner runner = new SimulationRunner(new RandomHuntStrategy(), 77);
        SimulationStats stats = runner.run(1_000);
        assertEquals(1_000, stats.games());
        assertTrue(stats.minTurns() >= 17);
        assertTrue(stats.maxTurns() <= 100);
        assertFalse(Double.isNaN(stats.meanTurns()));
    }
}
