package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import optimized.engine.BitBoard100;
import optimized.engine.OptimizedGame;
import optimized.engine.ShotResult;

class IncrementalBonusInvariantTest {
    private IncrementalBonusBoostedNoHuntStrategy strategy;

    @BeforeEach
    void reset() {
        strategy = new IncrementalBonusBoostedNoHuntStrategy();
        strategy.reset(new SplittableRandom(3));
    }

    @Test
    void targetedHitLinesAndBoundariesMatchFrozenReference() {
        verify();
        hit(0);       // corner
        hit(1);       // horizontal pair
        hit(2);       // horizontal triple
        hit(3);       // four-hit line
        hit(13);      // intersecting/nonaligned
        hit(23);      // vertical pair
        hit(33);      // vertical triple
        hit(63);      // below split
        hit(64);      // above split, adjacent horizontally
        verify();
    }

    @Test
    void sinksRemoveOnlyTheirOwnBonusDependencies() {
        hit(43);
        hit(44);
        hit(53); // nearby hit belonging to another ship
        sink(3, 43, 44, 45);
        assertEquals(1, strategy.adjacentHitCountAt(43));
        verify();

        hit(63);
        hit(64);
        sink(3, 63, 64, 65); // second length-three sink
        verify();
        hit(90);
        sink(2, 90, 91);
        verify();
        hit(6);
        hit(16);
        hit(26);
        hit(36);
        sink(5, 6, 16, 26, 36, 46);
        verify();
    }

    @Test
    void completeGamesMatchReferenceAfterEveryTransition() {
        SplittableRandom random = new SplittableRandom(55);
        OptimizedGame game = new OptimizedGame();
        for (int fleet = 0; fleet < 200; fleet++) {
            game.resetRandom(random);
            strategy.reset(random);
            verify();
            while (!game.isComplete()) {
                int shot = strategy.chooseShot();
                int result = game.shoot(shot);
                strategy.trackShot(shot, result, game.lastSunkLo(), game.lastSunkHi());
                verify();
            }
        }
    }

    @Test
    void randomizedValidObservationSequencesMatchPhase2AndReference() {
        SplittableRandom random = new SplittableRandom(8080);
        OptimizedGame game = new OptimizedGame();
        IncrementalBoostedNoHuntStrategy phase2 = new IncrementalBoostedNoHuntStrategy();
        int[] available = new int[100];
        for (int fleet = 0; fleet < 500; fleet++) {
            game.resetRandom(random);
            strategy.reset(random);
            phase2.reset(random);
            for (int cell = 0; cell < 100; cell++) available[cell] = cell;
            int count = 100;
            while (!game.isComplete()) {
                int selected = random.nextInt(count);
                int shot = available[selected];
                available[selected] = available[--count];
                int result = game.shoot(shot);
                phase2.trackShot(shot, result, game.lastSunkLo(), game.lastSunkHi());
                strategy.trackShot(shot, result, game.lastSunkLo(), game.lastSunkHi());
                verify();
                if (!game.isComplete()) assertEquals(phase2.chooseShot(), strategy.chooseShot());
            }
        }
    }

    private void verify() {
        strategy.verifyBonusInvariants();
        int[] reference = frozenBonus(strategy.hitsLo(), strategy.hitsHi(),
                strategy.shotsLo(), strategy.shotsHi());
        for (int cell = 0; cell < 100; cell++) {
            boolean unknown = !BitBoard100.contains(strategy.shotsLo(), strategy.shotsHi(), cell);
            if (unknown) assertEquals(reference[cell], strategy.bonusScoreAt(cell), "cell " + cell);
        }
    }

    private void hit(int cell) {
        strategy.trackShot(cell, ShotResult.HIT, 0L, 0L);
        verify();
    }

    private void sink(int length, int... cells) {
        long lo = 0L;
        long hi = 0L;
        for (int cell : cells) {
            lo |= BitBoard100.lowMask(cell);
            hi |= BitBoard100.highMask(cell);
        }
        strategy.trackShot(cells[cells.length - 1], ShotResult.sunk(length), lo, hi);
        verify();
    }

    private static int[] frozenBonus(long hitsLo, long hitsHi, long shotsLo, long shotsHi) {
        int[] bonus = new int[100];
        for (int cell = 0; cell < 100; cell++) {
            if (!BitBoard100.contains(hitsLo, hitsHi, cell)) continue;
            int row = cell / 10;
            int col = cell % 10;
            boost(bonus, shotsLo, shotsHi, row + 1, col, 10);
            boost(bonus, shotsLo, shotsHi, row - 1, col, 10);
            boost(bonus, shotsLo, shotsHi, row, col + 1, 10);
            boost(bonus, shotsLo, shotsHi, row, col - 1, 10);
            if (row > 0 && BitBoard100.contains(hitsLo, hitsHi, cell - 10)) {
                boost(bonus, shotsLo, shotsHi, row + 1, col, 15);
                if (row > 1 && BitBoard100.contains(hitsLo, hitsHi, cell - 20))
                    boost(bonus, shotsLo, shotsHi, row + 2, col, 30);
            }
            if (row < 9 && BitBoard100.contains(hitsLo, hitsHi, cell + 10)) {
                boost(bonus, shotsLo, shotsHi, row - 1, col, 15);
                if (row < 8 && BitBoard100.contains(hitsLo, hitsHi, cell + 20))
                    boost(bonus, shotsLo, shotsHi, row - 2, col, 30);
            }
            if (col > 0 && BitBoard100.contains(hitsLo, hitsHi, cell - 1)) {
                boost(bonus, shotsLo, shotsHi, row, col + 1, 15);
                if (col > 1 && BitBoard100.contains(hitsLo, hitsHi, cell - 2))
                    boost(bonus, shotsLo, shotsHi, row, col + 2, 30);
            }
            if (col < 9 && BitBoard100.contains(hitsLo, hitsHi, cell + 1)) {
                boost(bonus, shotsLo, shotsHi, row, col - 1, 15);
                if (col < 8 && BitBoard100.contains(hitsLo, hitsHi, cell + 2))
                    boost(bonus, shotsLo, shotsHi, row, col - 2, 30);
            }
        }
        return bonus;
    }

    private static void boost(int[] bonus, long shotsLo, long shotsHi,
                              int row, int col, int value) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10) return;
        int cell = row * 10 + col;
        if (!BitBoard100.contains(shotsLo, shotsHi, cell)) bonus[cell] += value;
    }
}
