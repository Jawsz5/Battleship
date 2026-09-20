package optimized.engine;

/** Allocation-free shot encoding: low two bits are the event, upper bits are sunk length. */
public final class ShotResult {
    public static final int MISS = 0;
    public static final int HIT = 1;
    public static final int SUNK = 2;
    private static final int LENGTH_SHIFT = 2;

    private ShotResult() {}

    public static int sunk(int shipLength) {
        return SUNK | (shipLength << LENGTH_SHIFT);
    }

    public static int kind(int result) {
        return result & 3;
    }

    public static int sunkLength(int result) {
        return kind(result) == SUNK ? result >>> LENGTH_SHIFT : 0;
    }

    public static boolean isHit(int result) {
        return kind(result) != MISS;
    }
}
