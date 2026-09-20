package optimized.engine;

import java.util.random.RandomGenerator;

/** Reusable, fixed-size Battleship state machine backed by two-long bitboards. */
public final class OptimizedGame {
    public static final int SHIP_COUNT = 5;
    public static final int[] SHIP_LENGTHS = {5, 4, 3, 3, 2};

    private final long[] shipLo = new long[SHIP_COUNT];
    private final long[] shipHi = new long[SHIP_COUNT];
    private long shotsLo;
    private long shotsHi;
    private long remainingLo;
    private long remainingHi;
    private long lastSunkLo;
    private long lastSunkHi;
    private int sunkShips;

    public void resetRandom(RandomGenerator random) {
        FleetGenerator.generate(this, random);
    }

    public void resetWithFleet(long[] lowMasks, long[] highMasks) {
        if (lowMasks.length != SHIP_COUNT || highMasks.length != SHIP_COUNT) {
            throw new IllegalArgumentException("A fleet must contain exactly five ships");
        }
        beginReset();
        for (int ship = 0; ship < SHIP_COUNT; ship++) {
            installShip(ship, lowMasks[ship], highMasks[ship]);
        }
        finishReset();
    }

    public int shoot(int cell) {
        if (cell < 0 || cell >= BitBoard100.CELL_COUNT) {
            throw new IllegalArgumentException("X-coordinate is out of bounds");
        }
        long cellLo = BitBoard100.lowMask(cell);
        long cellHi = BitBoard100.highMask(cell);
        if (((shotsLo & cellLo) | (shotsHi & cellHi)) != 0L) {
            throw new IllegalArgumentException("Spot has already been shot at");
        }

        shotsLo |= cellLo;
        shotsHi |= cellHi;
        lastSunkLo = 0L;
        lastSunkHi = 0L;

        if (((remainingLo & cellLo) | (remainingHi & cellHi)) == 0L) return ShotResult.MISS;

        int hitShip = 0;
        while (((shipLo[hitShip] & cellLo) | (shipHi[hitShip] & cellHi)) == 0L) hitShip++;
        remainingLo &= ~cellLo;
        remainingHi &= ~cellHi;

        if (((shipLo[hitShip] & remainingLo) | (shipHi[hitShip] & remainingHi)) == 0L) {
            sunkShips |= 1 << hitShip;
            lastSunkLo = shipLo[hitShip];
            lastSunkHi = shipHi[hitShip];
            return ShotResult.sunk(SHIP_LENGTHS[hitShip]);
        }
        return ShotResult.HIT;
    }

    public boolean isComplete() {
        return (remainingLo | remainingHi) == 0L;
    }

    public boolean isShipSunk(int ship) {
        if (ship < 0 || ship >= SHIP_COUNT) throw new IllegalArgumentException("Invalid ship index");
        return (sunkShips & (1 << ship)) != 0;
    }

    public long shotsLo() { return shotsLo; }
    public long shotsHi() { return shotsHi; }
    public long remainingLo() { return remainingLo; }
    public long remainingHi() { return remainingHi; }
    public long lastSunkLo() { return lastSunkLo; }
    public long lastSunkHi() { return lastSunkHi; }

    void beginReset() {
        shotsLo = shotsHi = remainingLo = remainingHi = 0L;
        lastSunkLo = lastSunkHi = 0L;
        sunkShips = 0;
    }

    void installShip(int ship, long lo, long hi) {
        if ((hi & ~BitBoard100.HIGH_MASK) != 0L) {
            throw new IllegalArgumentException("Fleet contains bits outside the 100-cell board");
        }
        if (Long.bitCount(lo) + Long.bitCount(hi) != SHIP_LENGTHS[ship]) {
            throw new IllegalArgumentException("Ship mask has the wrong length");
        }
        if (((remainingLo & lo) | (remainingHi & hi)) != 0L) {
            throw new IllegalArgumentException("Ships overlap");
        }
        shipLo[ship] = lo;
        shipHi[ship] = hi;
        remainingLo |= lo;
        remainingHi |= hi;
    }

    void finishReset() {
        remainingHi &= BitBoard100.HIGH_MASK;
    }
}
