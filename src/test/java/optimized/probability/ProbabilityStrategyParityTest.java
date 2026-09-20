package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.SplittableRandom;
import legacy.compstrategies.probstrats.HitMaxOptimize.BoostedNoHuntProb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import optimized.engine.BitBoard100;
import optimized.engine.ShotResult;

class ProbabilityStrategyParityTest {
    private BoostedNoHuntProb legacy;
    private RecomputeBoostedNoHuntStrategy recompute;
    private IncrementalBoostedNoHuntStrategy incremental;

    @BeforeEach
    void reset() {
        legacy = new BoostedNoHuntProb(10);
        recompute = new RecomputeBoostedNoHuntStrategy();
        incremental = new IncrementalBoostedNoHuntStrategy();
        SplittableRandom random = new SplittableRandom(1);
        recompute.reset(random);
        incremental.reset(random);
    }

    @Test
    void targetedObservationStatesChooseExactlyTheSameShot() {
        assertChoices("empty board");
        miss(44);
        assertChoices("one miss");
        hit(0);
        assertChoices("corner hit");
        hit(63);
        assertChoices("hit below split");
        hit(64);
        assertChoices("two aligned hits crossing split");
        hit(65);
        assertChoices("three aligned hits");
        hit(76);
        assertChoices("nonaligned hit");
        hit(99);
        assertChoices("opposite corner hit");
    }

    @Test
    void everySinkLengthIncludingBothThreesMatches() {
        sink(2, 10, 11);
        assertChoices("length two sunk");
        sink(3, 20, 21, 22);
        assertChoices("first length three sunk");
        sink(3, 30, 40, 50);
        assertChoices("second length three sunk");
        sink(4, 60, 61, 62, 63);
        assertChoices("length four sunk");
        sink(5, 5, 15, 25, 35, 45);
        assertChoices("length five sunk");
    }

    @Test
    void candidateScoresMatchAfterMultipleBonusContributions() {
        hit(44);
        hit(45);
        hit(34);
        miss(43);
        for (int cell = 0; cell < 100; cell++) {
            if (cell == 44 || cell == 45 || cell == 34 || cell == 43) continue;
            assertEquals(recompute.scoreAt(cell), incremental.candidateScoreAt(cell), "cell " + cell);
        }
        assertChoices("multiple bonus contributions");
    }

    @Test
    void initialTieKeepsFirstCellInLegacyScanOrder() {
        assertEquals(44, legacy.selectShot());
        assertEquals(44, recompute.chooseShot());
        assertEquals(44, incremental.chooseShot());
    }

    private void assertChoices(String state) {
        assertEquals(legacy.selectShot(), recompute.chooseShot(), "legacy/recompute: " + state);
        assertEquals(legacy.selectShot(), incremental.chooseShot(), "legacy/incremental: " + state);
    }

    private void miss(int cell) {
        legacy.trackShot(false, 0, cell, null);
        recompute.trackShot(cell, ShotResult.MISS, 0L, 0L);
        incremental.trackShot(cell, ShotResult.MISS, 0L, 0L);
    }

    private void hit(int cell) {
        legacy.trackShot(true, 0, cell, null);
        recompute.trackShot(cell, ShotResult.HIT, 0L, 0L);
        incremental.trackShot(cell, ShotResult.HIT, 0L, 0L);
    }

    private void sink(int length, int... cells) {
        long lo = 0L;
        long hi = 0L;
        for (int cell : cells) {
            lo |= BitBoard100.lowMask(cell);
            hi |= BitBoard100.highMask(cell);
        }
        int shot = cells[cells.length - 1];
        legacy.trackShot(true, length, shot, cells);
        int result = ShotResult.sunk(length);
        recompute.trackShot(shot, result, lo, hi);
        incremental.trackShot(shot, result, lo, hi);
    }
}
