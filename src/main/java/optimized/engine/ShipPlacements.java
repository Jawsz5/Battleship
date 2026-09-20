package optimized.engine;

/** Precomputed contiguous primitive placement tables for lengths 2 through 5. */
public final class ShipPlacements {
    private static final long[][] LO = new long[6][];
    private static final long[][] HI = new long[6][];

    static {
        for (int length = 2; length <= 5; length++) build(length);
    }

    private ShipPlacements() {}

    private static void build(int length) {
        int count = 2 * BitBoard100.WIDTH * (BitBoard100.WIDTH + 1 - length);
        long[] lo = new long[count];
        long[] hi = new long[count];
        int index = 0;

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col <= 10 - length; col++) {
                for (int offset = 0; offset < length; offset++) {
                    int cell = row * 10 + col + offset;
                    lo[index] |= BitBoard100.lowMask(cell);
                    hi[index] |= BitBoard100.highMask(cell);
                }
                index++;
            }
        }
        for (int col = 0; col < 10; col++) {
            for (int row = 0; row <= 10 - length; row++) {
                for (int offset = 0; offset < length; offset++) {
                    int cell = (row + offset) * 10 + col;
                    lo[index] |= BitBoard100.lowMask(cell);
                    hi[index] |= BitBoard100.highMask(cell);
                }
                index++;
            }
        }
        LO[length] = lo;
        HI[length] = hi;
    }

    public static int countForLength(int length) {
        checkLength(length);
        return LO[length].length;
    }

    public static long lowMask(int length, int placement) {
        checkLength(length);
        return LO[length][placement];
    }

    public static long highMask(int length, int placement) {
        checkLength(length);
        return HI[length][placement];
    }

    static long low(int length, int placement) {
        return lowMask(length, placement);
    }

    static long high(int length, int placement) {
        return highMask(length, placement);
    }

    private static void checkLength(int length) {
        if (length < 2 || length > 5) throw new IllegalArgumentException("Ship length must be 2..5");
    }
}
