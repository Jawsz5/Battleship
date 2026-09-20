package optimized.probability;

import optimized.engine.BitBoard100;

/** Immutable dependency graph mechanically generated from the frozen bonus loop. */
public final class BonusMotifGraph {
    public static final int ADJACENCY_MOTIFS = 360;
    public static final int LINE_15_MOTIFS = 320;
    public static final int LINE_30_MOTIFS = 240;
    public static final int MOTIF_COUNT = 920;
    public static final int HIT_MOTIF_INCIDENCES = 1_720;
    public static final int ACTIVE_WORD_COUNT = (MOTIF_COUNT + 63) >>> 6;

    static final long[] REQUIRED_LO = new long[MOTIF_COUNT];
    static final long[] REQUIRED_HI = new long[MOTIF_COUNT];
    static final byte[] TARGET = new byte[MOTIF_COUNT];
    static final byte[] WEIGHT = new byte[MOTIF_COUNT];
    static final int[] HIT_MOTIF_OFFSET = new int[101];
    static final int[] HIT_MOTIFS = new int[HIT_MOTIF_INCIDENCES];

    static {
        int motif = 0;
        int adjacencyEnd;
        int line15End;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        // Frozen boost(neighbor, ADJ): one directed motif for every hit -> neighbor relation.
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                int hit = row * 10 + col;
                for (int[] direction : directions) {
                    int targetRow = row + direction[0];
                    int targetCol = col + direction[1];
                    if (inside(targetRow, targetCol)) motif = add(motif, hit, -1, -1,
                            targetRow * 10 + targetCol, 10);
                }
            }
        }
        adjacencyEnd = motif;

        // Frozen line rule: center and center+d are HIT, target is center-d.
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                int center = row * 10 + col;
                for (int[] direction : directions) {
                    int hitRow = row + direction[0];
                    int hitCol = col + direction[1];
                    int targetRow = row - direction[0];
                    int targetCol = col - direction[1];
                    if (inside(hitRow, hitCol) && inside(targetRow, targetCol)) {
                        motif = add(motif, center, hitRow * 10 + hitCol, -1,
                                targetRow * 10 + targetCol, 15);
                    }
                }
            }
        }
        line15End = motif;

        // Frozen LINE*2 rule: center, center+d, center+2d are HIT; target is center-2d.
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                int center = row * 10 + col;
                for (int[] direction : directions) {
                    int hit1Row = row + direction[0];
                    int hit1Col = col + direction[1];
                    int hit2Row = row + 2 * direction[0];
                    int hit2Col = col + 2 * direction[1];
                    int targetRow = row - 2 * direction[0];
                    int targetCol = col - 2 * direction[1];
                    if (inside(hit1Row, hit1Col) && inside(hit2Row, hit2Col)
                            && inside(targetRow, targetCol)) {
                        motif = add(motif, center, hit1Row * 10 + hit1Col,
                                hit2Row * 10 + hit2Col, targetRow * 10 + targetCol, 30);
                    }
                }
            }
        }

        if (adjacencyEnd != ADJACENCY_MOTIFS
                || line15End - adjacencyEnd != LINE_15_MOTIFS
                || motif - line15End != LINE_30_MOTIFS
                || motif != MOTIF_COUNT) {
            throw new ExceptionInInitializerError("Unexpected bonus motif counts");
        }

        int[] counts = new int[100];
        for (int m = 0; m < MOTIF_COUNT; m++) {
            long lo = REQUIRED_LO[m];
            long hi = REQUIRED_HI[m];
            while (lo != 0L) {
                counts[Long.numberOfTrailingZeros(lo)]++;
                lo &= lo - 1;
            }
            while (hi != 0L) {
                counts[64 + Long.numberOfTrailingZeros(hi)]++;
                hi &= hi - 1;
            }
        }
        for (int cell = 0; cell < 100; cell++) {
            HIT_MOTIF_OFFSET[cell + 1] = HIT_MOTIF_OFFSET[cell] + counts[cell];
        }
        if (HIT_MOTIF_OFFSET[100] != HIT_MOTIF_INCIDENCES) {
            throw new ExceptionInInitializerError("Unexpected hit/motif incidence count");
        }
        int[] cursor = HIT_MOTIF_OFFSET.clone();
        for (int m = 0; m < MOTIF_COUNT; m++) {
            long lo = REQUIRED_LO[m];
            long hi = REQUIRED_HI[m];
            while (lo != 0L) {
                int cell = Long.numberOfTrailingZeros(lo);
                HIT_MOTIFS[cursor[cell]++] = m;
                lo &= lo - 1;
            }
            while (hi != 0L) {
                int cell = 64 + Long.numberOfTrailingZeros(hi);
                HIT_MOTIFS[cursor[cell]++] = m;
                hi &= hi - 1;
            }
        }
    }

    private BonusMotifGraph() {}

    private static int add(int motif, int hit0, int hit1, int hit2, int target, int weight) {
        long lo = BitBoard100.lowMask(hit0);
        long hi = BitBoard100.highMask(hit0);
        if (hit1 >= 0) {
            lo |= BitBoard100.lowMask(hit1);
            hi |= BitBoard100.highMask(hit1);
        }
        if (hit2 >= 0) {
            lo |= BitBoard100.lowMask(hit2);
            hi |= BitBoard100.highMask(hit2);
        }
        REQUIRED_LO[motif] = lo;
        REQUIRED_HI[motif] = hi;
        TARGET[motif] = (byte) target;
        WEIGHT[motif] = (byte) weight;
        return motif + 1;
    }

    private static boolean inside(int row, int col) {
        return row >= 0 && row < 10 && col >= 0 && col < 10;
    }

    public static int hitMotifOffsetSize() { return HIT_MOTIF_OFFSET.length; }
    public static int hitMotifsSize() { return HIT_MOTIFS.length; }
}
