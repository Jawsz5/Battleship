package optimized.engine;

import java.util.random.RandomGenerator;

/** Exception-free random selection from legal precomputed ship placements. */
final class FleetGenerator {
    private FleetGenerator() {}

    static void generate(OptimizedGame game, RandomGenerator random) {
        game.beginReset();
        long occupiedLo = 0L;
        long occupiedHi = 0L;
        for (int ship = 0; ship < OptimizedGame.SHIP_COUNT; ship++) {
            int length = OptimizedGame.SHIP_LENGTHS[ship];
            while (true) {
                int placement = random.nextInt(ShipPlacements.countForLength(length));
                long lo = ShipPlacements.low(length, placement);
                long hi = ShipPlacements.high(length, placement);
                if (((occupiedLo & lo) | (occupiedHi & hi)) == 0L) {
                    game.installShip(ship, lo, hi);
                    occupiedLo |= lo;
                    occupiedHi |= hi;
                    break;
                }
            }
        }
        game.finishReset();
    }
}
