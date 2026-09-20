package optimized.engine;

/** Utilities for the fixed row-major 10x10 board split at cell 64. */
public final class BitBoard100 {
    public static final int WIDTH = 10;
    public static final int CELL_COUNT = 100;
    public static final long LOW_MASK = -1L;
    public static final long HIGH_MASK = (1L << 36) - 1L;

    private BitBoard100() {}

    public static long lowMask(int cell) {
        return cell < 64 ? 1L << cell : 0L;
    }

    public static long highMask(int cell) {
        return cell >= 64 ? 1L << (cell - 64) : 0L;
    }

    public static boolean contains(long lo, long hi, int cell) {
        return ((lo & lowMask(cell)) | (hi & highMask(cell))) != 0L;
    }

    public static int bitCount(long lo, long hi) {
        return Long.bitCount(lo) + Long.bitCount(hi & HIGH_MASK);
    }

    /** Returns the zero-based nth set cell without allocating. */
    public static int nthSetCell(long lo, long hi, int n) {
        int lowCount = Long.bitCount(lo);
        if (n < lowCount) return nthBit(lo, n);
        return 64 + nthBit(hi & HIGH_MASK, n - lowCount);
    }

    private static int nthBit(long bits, int n) {
        while (n-- > 0) bits &= bits - 1;
        if (bits == 0L) throw new IllegalArgumentException("Set-bit index is out of range");
        return Long.numberOfTrailingZeros(bits);
    }
}
