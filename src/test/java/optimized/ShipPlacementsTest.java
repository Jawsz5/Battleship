package optimized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import optimized.engine.BitBoard100;
import optimized.engine.OptimizedGame;
import optimized.engine.ShipPlacements;

class ShipPlacementsTest {
    @Test
    void placementCountsMatchTenByTenGeometry() {
        assertEquals(180, ShipPlacements.countForLength(2));
        assertEquals(160, ShipPlacements.countForLength(3));
        assertEquals(140, ShipPlacements.countForLength(4));
        assertEquals(120, ShipPlacements.countForLength(5));
    }

    @Test
    void randomFleetAlwaysContainsSeventeenValidNonOverlappingCells() {
        OptimizedGame game = new OptimizedGame();
        SplittableRandom random = new SplittableRandom(9917);
        for (int trial = 0; trial < 10_000; trial++) {
            game.resetRandom(random);
            assertEquals(17, BitBoard100.bitCount(game.remainingLo(), game.remainingHi()));
            assertEquals(0L, game.remainingHi() & ~BitBoard100.HIGH_MASK);
        }
    }

    @Test
    void invalidHighBitsAreRejected() {
        long[] lo = TestFleet.low();
        long[] hi = TestFleet.high();
        hi[4] |= 1L << 40;
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new OptimizedGame().resetWithFleet(lo, hi));
        assertTrue(error.getMessage().contains("outside"));
    }
}
