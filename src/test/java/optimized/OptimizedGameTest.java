package optimized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import optimized.engine.BitBoard100;
import optimized.engine.OptimizedGame;
import optimized.engine.ShotResult;

class OptimizedGameTest {
    private OptimizedGame game;

    @BeforeEach
    void reset() {
        game = new OptimizedGame();
        game.resetWithFleet(TestFleet.low(), TestFleet.high());
    }

    @Test
    void resolvesMissHitSinkAndCompletion() {
        assertEquals(ShotResult.MISS, game.shoot(5));
        assertEquals(ShotResult.HIT, game.shoot(0));
        game.shoot(1);
        game.shoot(2);
        game.shoot(3);
        int sinkFive = game.shoot(4);
        assertEquals(ShotResult.SUNK, ShotResult.kind(sinkFive));
        assertEquals(5, ShotResult.sunkLength(sinkFive));
        assertEquals(TestFleet.low()[0], game.lastSunkLo());

        int[] rest = {6, 7, 8, 9, 10, 20, 30, 63, 64, 65, 90, 91};
        for (int cell : rest) game.shoot(cell);
        assertTrue(game.isComplete());
        for (int ship = 0; ship < 5; ship++) assertTrue(game.isShipSunk(ship));
    }

    @Test
    void repeatedAndOutOfRangeShotsMatchLegacyExceptions() {
        game.shoot(42);
        assertThrows(IllegalArgumentException.class, () -> game.shoot(42));
        assertThrows(IllegalArgumentException.class, () -> game.shoot(-1));
        assertThrows(IllegalArgumentException.class, () -> game.shoot(100));
    }

    @Test
    void splitBoundaryUsesIndependentBits() {
        assertEquals(ShotResult.HIT, game.shoot(63));
        assertEquals(ShotResult.HIT, game.shoot(64));
        int result = game.shoot(65);
        assertEquals(3, ShotResult.sunkLength(result));
        assertTrue(BitBoard100.contains(game.shotsLo(), game.shotsHi(), 63));
        assertTrue(BitBoard100.contains(game.shotsLo(), game.shotsHi(), 64));
        assertEquals(0L, game.shotsHi() & ~BitBoard100.HIGH_MASK);
    }

    @Test
    void namedEdgeAndBoundaryCellsResolveCorrectly() {
        int[] hits = {0, 9, 10, 63, 64, 90};
        for (int cell : hits) assertTrue(ShotResult.isHit(game.shoot(cell)), "cell " + cell);
        assertEquals(ShotResult.MISS, game.shoot(99));
    }

    @Test
    void resetReusesAndClearsState() {
        game.shoot(0);
        game.shoot(99);
        game.resetWithFleet(TestFleet.low(), TestFleet.high());
        assertEquals(0L, game.shotsLo());
        assertEquals(0L, game.shotsHi());
        assertFalse(game.isComplete());
        assertEquals(17, BitBoard100.bitCount(game.remainingLo(), game.remainingHi()));
    }

    @Test
    void bottomRightHorizontalShipSinksAtNinetyNine() {
        long[] lo = TestFleet.low();
        long[] hi = TestFleet.high();
        lo[4] = TestFleet.low(98, 99);
        hi[4] = TestFleet.high(98, 99);
        game.resetWithFleet(lo, hi);
        assertEquals(ShotResult.HIT, game.shoot(98));
        assertEquals(2, ShotResult.sunkLength(game.shoot(99)));
    }
}
