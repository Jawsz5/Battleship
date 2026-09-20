package optimized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import legacy.gamefiles.Ocean;
import legacy.gamefiles.Ship;
import org.junit.jupiter.api.Test;
import optimized.engine.OptimizedGame;
import optimized.engine.ShotResult;

class LegacyEngineParityTest {
    @Test
    void explicitFleetAndShotSequenceMatchLegacy() throws Exception {
        LegacyAdapter legacy = new LegacyAdapter();
        OptimizedGame optimized = new OptimizedGame();
        optimized.resetWithFleet(TestFleet.low(), TestFleet.high());
        int[] sequence = {5, 99, 98, 0, 2, 1, 4, 3, 9, 6, 8, 7, 10, 30, 20,
                63, 65, 64, 90, 91};

        for (int shot : sequence) {
            assertEquals(legacy.shoot(shot), optimized.shoot(shot), "shot " + shot);
            assertEquals(legacy.complete(), optimized.isComplete(), "completion after " + shot);
        }
    }

    @Test
    void repeatedShotIsRejectedByBothEngines() throws Exception {
        LegacyAdapter legacy = new LegacyAdapter();
        OptimizedGame optimized = new OptimizedGame();
        optimized.resetWithFleet(TestFleet.low(), TestFleet.high());
        legacy.shoot(42);
        optimized.shoot(42);
        assertThrows(IllegalArgumentException.class, () -> legacy.shoot(42));
        assertThrows(IllegalArgumentException.class, () -> optimized.shoot(42));
    }

    private static final class LegacyAdapter {
        private final Ocean ocean;
        private final ArrayList<Ship> ships;
        private final boolean[] shots = new boolean[100];
        private final boolean[] reportedSunk = new boolean[5];

        LegacyAdapter() throws Exception {
            ocean = new Ocean(10);
            ocean.addBoats(0, false, "aircraftcarrier");
            ocean.addBoats(6, false, "battleship");
            ocean.addBoats(10, true, "destroyer");
            ocean.addBoats(63, false, "cruiser");
            ocean.addBoats(90, false, "submarine");
            ships = ocean.getBoats();
        }

        int shoot(int position) {
            if (position < 0 || position >= 100) throw new IllegalArgumentException("X-coordinate is out of bounds");
            if (shots[position]) throw new IllegalArgumentException("Spot has already been shot at");
            shots[position] = true;
            if (!ocean.isHit(position)) return ShotResult.MISS;

            for (Ship ship : ships) ship.hit(position);
            for (int index = 0; index < ships.size(); index++) {
                if (!reportedSunk[index] && ships.get(index).isSunk()) {
                    reportedSunk[index] = true;
                    return ShotResult.sunk(ships.get(index).getBoatLength());
                }
            }
            return ShotResult.HIT;
        }

        boolean complete() { return ocean.isAllSunk(); }
    }
}
