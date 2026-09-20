package optimized.probability;

import optimized.engine.BitBoard100;
import optimized.engine.ShipPlacements;

/** Immutable 600-placement, 2,000-incidence graph derived from Phase 1 placement masks. */
public final class PlacementCsr {
    public static final int PLACEMENT_COUNT = 600;
    public static final int INCIDENCE_COUNT = 2_000;
    public static final int ACTIVE_WORD_COUNT = 10;

    static final int[] PLACEMENT_CELL_OFFSET = new int[PLACEMENT_COUNT + 1];
    static final int[] PLACEMENT_CELLS = new int[INCIDENCE_COUNT];
    static final byte[] PLACEMENT_LENGTH = new byte[PLACEMENT_COUNT];
    static final int[] CELL_PLACEMENT_OFFSET = new int[BitBoard100.CELL_COUNT + 1];
    static final int[] CELL_PLACEMENTS = new int[INCIDENCE_COUNT];
    static final long[] INITIAL_ACTIVE = new long[ACTIVE_WORD_COUNT];
    static final int[] INITIAL_BASE_SCORE = new int[BitBoard100.CELL_COUNT];
    static final int[] INITIAL_COVERAGE = new int[6 * BitBoard100.CELL_COUNT];

    static {
        int placement = 0;
        int incidence = 0;
        int[] cellCounts = new int[BitBoard100.CELL_COUNT];

        for (int length = 2; length <= 5; length++) {
            int count = ShipPlacements.countForLength(length);
            for (int local = 0; local < count; local++) {
                PLACEMENT_CELL_OFFSET[placement] = incidence;
                PLACEMENT_LENGTH[placement] = (byte) length;
                long lo = ShipPlacements.lowMask(length, local);
                long hi = ShipPlacements.highMask(length, local);
                while (lo != 0L) {
                    int cell = Long.numberOfTrailingZeros(lo);
                    PLACEMENT_CELLS[incidence++] = cell;
                    cellCounts[cell]++;
                    lo &= lo - 1;
                }
                while (hi != 0L) {
                    int cell = 64 + Long.numberOfTrailingZeros(hi);
                    PLACEMENT_CELLS[incidence++] = cell;
                    cellCounts[cell]++;
                    hi &= hi - 1;
                }
                placement++;
            }
        }
        PLACEMENT_CELL_OFFSET[placement] = incidence;
        if (placement != PLACEMENT_COUNT || incidence != INCIDENCE_COUNT) {
            throw new ExceptionInInitializerError("Unexpected placement graph size");
        }

        for (int cell = 0; cell < BitBoard100.CELL_COUNT; cell++) {
            CELL_PLACEMENT_OFFSET[cell + 1] = CELL_PLACEMENT_OFFSET[cell] + cellCounts[cell];
        }
        int[] cursors = CELL_PLACEMENT_OFFSET.clone();
        for (int p = 0; p < PLACEMENT_COUNT; p++) {
            int length = PLACEMENT_LENGTH[p];
            int weight = initialWeight(length);
            INITIAL_ACTIVE[p >>> 6] |= 1L << (p & 63);
            for (int at = PLACEMENT_CELL_OFFSET[p]; at < PLACEMENT_CELL_OFFSET[p + 1]; at++) {
                int cell = PLACEMENT_CELLS[at];
                CELL_PLACEMENTS[cursors[cell]++] = p;
                INITIAL_COVERAGE[length * 100 + cell]++;
                INITIAL_BASE_SCORE[cell] += weight;
            }
        }
    }

    private PlacementCsr() {}

    private static int initialWeight(int length) {
        return length == 3 ? 2 : 1;
    }

    public static int placementOffsetSize() { return PLACEMENT_CELL_OFFSET.length; }
    public static int placementCellsSize() { return PLACEMENT_CELLS.length; }
    public static int placementLengthSize() { return PLACEMENT_LENGTH.length; }
    public static int cellOffsetSize() { return CELL_PLACEMENT_OFFSET.length; }
    public static int cellPlacementsSize() { return CELL_PLACEMENTS.length; }
}
