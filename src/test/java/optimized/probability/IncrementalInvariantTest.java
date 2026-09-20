package optimized.probability;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import optimized.engine.BitBoard100;
import optimized.engine.OptimizedGame;
import optimized.engine.ShotResult;

class IncrementalInvariantTest {
    @Test
    void activeCoverageAndBaseRemainSynchronizedAcrossUpdates() {
        IncrementalBoostedNoHuntStrategy strategy = new IncrementalBoostedNoHuntStrategy();
        strategy.reset(new SplittableRandom(7));
        strategy.verifyInvariants();

        strategy.trackShot(44, ShotResult.HIT, 0L, 0L);
        strategy.verifyInvariants();
        strategy.trackShot(0, ShotResult.MISS, 0L, 0L);
        strategy.verifyInvariants();
        strategy.trackShot(99, ShotResult.MISS, 0L, 0L);
        strategy.verifyInvariants();

        sink(strategy, 3, 43, 44, 45);
        strategy.verifyInvariants();
        sink(strategy, 3, 63, 64, 65);
        strategy.verifyInvariants();
        sink(strategy, 2, 10, 20);
        strategy.verifyInvariants();
        sink(strategy, 4, 70, 71, 72, 73);
        strategy.verifyInvariants();
        sink(strategy, 5, 6, 16, 26, 36, 46);
        strategy.verifyInvariants();
    }

    @Test
    void invariantsHoldAfterEveryTurnAcrossDeterministicGames() {
        SplittableRandom random = new SplittableRandom(99);
        OptimizedGame game = new OptimizedGame();
        IncrementalBoostedNoHuntStrategy strategy = new IncrementalBoostedNoHuntStrategy();
        for (int fleet = 0; fleet < 100; fleet++) {
            game.resetRandom(random);
            strategy.reset(random);
            strategy.verifyInvariants();
            while (!game.isComplete()) {
                int shot = strategy.chooseShot();
                int result = game.shoot(shot);
                strategy.trackShot(shot, result, game.lastSunkLo(), game.lastSunkHi());
                strategy.verifyInvariants();
            }
        }
    }

    @Test
    void duplicateCsrVisitsOnlyInvalidateAPlacementOnce() {
        IncrementalBoostedNoHuntStrategy strategy = new IncrementalBoostedNoHuntStrategy();
        strategy.reset(new SplittableRandom(8));
        strategy.trackShot(44, ShotResult.MISS, 0L, 0L);
        int afterFirst = strategy.activePlacementCount();
        strategy.verifyInvariants();
        strategy.trackShot(45, ShotResult.MISS, 0L, 0L);
        strategy.verifyInvariants();
        org.junit.jupiter.api.Assertions.assertTrue(strategy.activePlacementCount() < afterFirst);
    }

    private static void sink(IncrementalBoostedNoHuntStrategy strategy, int length, int... cells) {
        long lo = 0L;
        long hi = 0L;
        for (int cell : cells) {
            lo |= BitBoard100.lowMask(cell);
            hi |= BitBoard100.highMask(cell);
        }
        strategy.trackShot(cells[cells.length - 1], ShotResult.sunk(length), lo, hi);
    }
}
