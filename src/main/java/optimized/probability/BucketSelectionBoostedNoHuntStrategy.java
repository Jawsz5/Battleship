package optimized.probability;

import optimized.engine.BitBoard100;

import java.util.Arrays;

/** Score buckets keyed by score, adjacency state, and lowest set cell ID. */
public final class BucketSelectionBoostedNoHuntStrategy extends AbstractIncrementalSelectionStrategy {
    private static final int KEY_COUNT = SCORE_VALUE_COUNT * 2;
    private final long[] bucketLo = new long[KEY_COUNT];
    private final long[] bucketHi = new long[KEY_COUNT];
    private int currentMaxKey;

    @Override
    protected void resetRankingStructure() {
        Arrays.fill(bucketLo, 0L);
        Arrays.fill(bucketHi, 0L);
        currentMaxKey = 0;
        for (int cell = 0; cell < 100; cell++) insert(cell, indexedScore[cell] & 0xFFFF, false);
    }

    @Override
    protected void reconcileRanking(int cell, int oldScore, boolean oldAdjacent,
                                    boolean oldSelectable, int newScore,
                                    boolean newAdjacent, boolean newSelectable) {
        int oldKey = (oldScore << 1) | (oldAdjacent ? 1 : 0);
        int newKey = (newScore << 1) | (newAdjacent ? 1 : 0);
        if (cell < 64) {
            long mask = 1L << cell;
            if (oldSelectable) bucketLo[oldKey] &= ~mask;
            if (newSelectable) bucketLo[newKey] |= mask;
        } else {
            long mask = 1L << (cell - 64);
            if (oldSelectable) bucketHi[oldKey] &= ~mask;
            if (newSelectable) bucketHi[newKey] |= mask;
        }
        if (newSelectable && newKey > currentMaxKey) currentMaxKey = newKey;
        while (currentMaxKey > 0 && bucketEmpty(currentMaxKey)) currentMaxKey--;
    }

    @Override
    protected int rankingBest() {
        long lo = bucketLo[currentMaxKey];
        long hi = bucketHi[currentMaxKey];
        if (lo != 0L) return Long.numberOfTrailingZeros(lo);
        return hi != 0L ? 64 + Long.numberOfTrailingZeros(hi) : -1;
    }

    private void insert(int cell, int score, boolean adjacent) {
        int key = (score << 1) | (adjacent ? 1 : 0);
        if (cell < 64) bucketLo[key] |= 1L << cell;
        else bucketHi[key] |= 1L << (cell - 64);
        if (key > currentMaxKey) currentMaxKey = key;
    }

    private boolean bucketEmpty(int key) {
        return (bucketLo[key] | bucketHi[key]) == 0L;
    }

    @Override
    protected void verifyStructure() {
        int seen = 0;
        for (int key = 0; key < KEY_COUNT; key++) {
            long lo = bucketLo[key];
            long hi = bucketHi[key];
            seen += Long.bitCount(lo) + Long.bitCount(hi);
            for (int cell = 0; cell < 100; cell++) {
                boolean present = BitBoard100.contains(lo, hi, cell);
                int expectedKey = ((indexedScore[cell] & 0xFFFF) << 1)
                        | (indexedAdjacent[cell] != 0 ? 1 : 0);
                boolean expected = indexedSelectable[cell] != 0 && expectedKey == key;
                if (present != expected) throw new IllegalStateException("bucket mismatch at " + cell);
            }
        }
        int selectable = 0;
        for (byte value : indexedSelectable) if (value != 0) selectable++;
        if (seen != selectable) throw new IllegalStateException("bucket candidate count mismatch");
    }

    @Override
    public String name() { return "BucketSelectionBoostedNoHunt"; }
}
