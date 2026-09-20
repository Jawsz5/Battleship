package optimized.probability;

import java.util.Arrays;
import java.util.random.RandomGenerator;
import optimized.engine.BitBoard100;
import optimized.engine.ShotResult;
import optimized.strategies.OptimizedStrategy;

/** Shared Phase 4 state updates; subclasses isolate only candidate ranking architecture. */
abstract class AbstractIncrementalSelectionStrategy implements OptimizedStrategy {
    static final int MIN_TOTAL_SCORE = 0;
    static final int MAX_BASE_SCORE = 34;
    static final int MAX_BONUS_SCORE = 220;
    static final int MAX_TOTAL_SCORE = MAX_BASE_SCORE + MAX_BONUS_SCORE;
    static final int SCORE_VALUE_COUNT = MAX_TOTAL_SCORE + 1;

    protected final int[] remain = new int[6];
    protected final long[] activePlacements = new long[PlacementCsr.ACTIVE_WORD_COUNT];
    protected final int[] baseScore = new int[100];
    protected final int[] coverage = new int[600];
    protected final long[] activeMotifs = new long[BonusMotifGraph.ACTIVE_WORD_COUNT];
    protected final long[] dirtyMotifs = new long[BonusMotifGraph.ACTIVE_WORD_COUNT];
    protected final int[] bonusScore = new int[100];
    protected final byte[] adjacentHitCount = new byte[100];

    protected final short[] indexedScore = new short[100];
    protected final byte[] indexedAdjacent = new byte[100];
    protected final byte[] indexedSelectable = new byte[100];
    protected long dirtyCellLo;
    protected long dirtyCellHi;

    protected long shotsLo;
    protected long shotsHi;
    protected long hitsLo;
    protected long hitsHi;
    protected long sunkLo;
    protected long sunkHi;

    @Override
    public final void reset(RandomGenerator random) {
        Arrays.fill(remain, 0);
        remain[2] = 1;
        remain[3] = 2;
        remain[4] = 1;
        remain[5] = 1;
        System.arraycopy(PlacementCsr.INITIAL_ACTIVE, 0, activePlacements, 0, activePlacements.length);
        System.arraycopy(PlacementCsr.INITIAL_BASE_SCORE, 0, baseScore, 0, baseScore.length);
        System.arraycopy(PlacementCsr.INITIAL_COVERAGE, 0, coverage, 0, coverage.length);
        Arrays.fill(activeMotifs, 0L);
        Arrays.fill(dirtyMotifs, 0L);
        Arrays.fill(bonusScore, 0);
        Arrays.fill(adjacentHitCount, (byte) 0);
        shotsLo = shotsHi = hitsLo = hitsHi = sunkLo = sunkHi = 0L;
        dirtyCellLo = dirtyCellHi = 0L;
        for (int cell = 0; cell < 100; cell++) {
            indexedScore[cell] = (short) baseScore[cell];
            indexedAdjacent[cell] = 0;
            indexedSelectable[cell] = 1;
        }
        resetRankingStructure();
    }

    protected abstract void resetRankingStructure();

    protected abstract void reconcileRanking(int cell, int oldScore, boolean oldAdjacent,
                                             boolean oldSelectable, int newScore,
                                             boolean newAdjacent, boolean newSelectable);

    protected abstract int rankingBest();

    protected abstract void verifyStructure();

    @Override
    public final int chooseShot() {
        int best = rankingBest();
        if (best < 0) throw new IllegalStateException("No cells left to fire at");
        return best;
    }

    @Override
    public final void trackShot(int position, int result, long reportedSunkLo, long reportedSunkHi) {
        long cellLo = BitBoard100.lowMask(position);
        long cellHi = BitBoard100.highMask(position);
        shotsLo |= cellLo;
        shotsHi |= cellHi;
        markDirty(position);
        int kind = ShotResult.kind(result);

        if (kind == ShotResult.MISS) {
            invalidateThroughCell(position);
            reconcileDirtyCells();
            return;
        }
        if (kind == ShotResult.HIT) {
            hitsLo |= cellLo;
            hitsHi |= cellHi;
            adjustAdjacentNeighbors(position, 1);
            reevaluateIncidentMotifs(cellLo, cellHi);
            reconcileDirtyCells();
            return;
        }

        int length = ShotResult.sunkLength(result);
        int coverageOffset = length * 100;
        for (int cell = 0; cell < 100; cell++) {
            int delta = coverage[coverageOffset + cell];
            if (delta != 0) {
                baseScore[cell] -= delta;
                markDirty(cell);
            }
        }
        remain[length]--;

        long removedHitLo = hitsLo & reportedSunkLo;
        long removedHitHi = hitsHi & reportedSunkHi;
        removeAdjacencyForHits(removedHitLo, removedHitHi);
        hitsLo &= ~reportedSunkLo;
        hitsHi &= ~reportedSunkHi;
        sunkLo |= reportedSunkLo;
        sunkHi |= reportedSunkHi;
        shotsLo |= reportedSunkLo;
        shotsHi |= reportedSunkHi;
        markDirtyMask(reportedSunkLo, reportedSunkHi);
        reevaluateIncidentMotifs(removedHitLo, removedHitHi);
        invalidateThroughMask(reportedSunkLo, reportedSunkHi);
        reconcileDirtyCells();
    }

    private void invalidateThroughMask(long lo, long hi) {
        while (lo != 0L) {
            int cell = Long.numberOfTrailingZeros(lo);
            invalidateThroughCell(cell);
            lo &= lo - 1;
        }
        while (hi != 0L) {
            int cell = 64 + Long.numberOfTrailingZeros(hi);
            invalidateThroughCell(cell);
            hi &= hi - 1;
        }
    }

    private void invalidateThroughCell(int cell) {
        int from = PlacementCsr.CELL_PLACEMENT_OFFSET[cell];
        int to = PlacementCsr.CELL_PLACEMENT_OFFSET[cell + 1];
        for (int at = from; at < to; at++) invalidatePlacement(PlacementCsr.CELL_PLACEMENTS[at]);
    }

    private void invalidatePlacement(int placement) {
        int word = placement >>> 6;
        long bit = 1L << (placement & 63);
        if ((activePlacements[word] & bit) == 0L) return;
        activePlacements[word] &= ~bit;
        int length = PlacementCsr.PLACEMENT_LENGTH[placement];
        int weight = remain[length];
        int coverageOffset = length * 100;
        int from = PlacementCsr.PLACEMENT_CELL_OFFSET[placement];
        int to = PlacementCsr.PLACEMENT_CELL_OFFSET[placement + 1];
        for (int at = from; at < to; at++) {
            int cell = PlacementCsr.PLACEMENT_CELLS[at];
            baseScore[cell] -= weight;
            coverage[coverageOffset + cell]--;
            markDirty(cell);
        }
    }

    private void reevaluateIncidentMotifs(long changedLo, long changedHi) {
        while (changedLo != 0L) {
            collectIncidentMotifs(Long.numberOfTrailingZeros(changedLo));
            changedLo &= changedLo - 1;
        }
        while (changedHi != 0L) {
            collectIncidentMotifs(64 + Long.numberOfTrailingZeros(changedHi));
            changedHi &= changedHi - 1;
        }
        for (int word = 0; word < dirtyMotifs.length; word++) {
            long dirty = dirtyMotifs[word];
            dirtyMotifs[word] = 0L;
            while (dirty != 0L) {
                int motif = (word << 6) + Long.numberOfTrailingZeros(dirty);
                if (motif < BonusMotifGraph.MOTIF_COUNT) reevaluateMotif(motif);
                dirty &= dirty - 1;
            }
        }
    }

    private void collectIncidentMotifs(int cell) {
        int from = BonusMotifGraph.HIT_MOTIF_OFFSET[cell];
        int to = BonusMotifGraph.HIT_MOTIF_OFFSET[cell + 1];
        for (int at = from; at < to; at++) {
            int motif = BonusMotifGraph.HIT_MOTIFS[at];
            dirtyMotifs[motif >>> 6] |= 1L << (motif & 63);
        }
    }

    private void reevaluateMotif(int motif) {
        boolean satisfied = (hitsLo & BonusMotifGraph.REQUIRED_LO[motif]) == BonusMotifGraph.REQUIRED_LO[motif]
                && (hitsHi & BonusMotifGraph.REQUIRED_HI[motif]) == BonusMotifGraph.REQUIRED_HI[motif];
        int word = motif >>> 6;
        long bit = 1L << (motif & 63);
        boolean wasActive = (activeMotifs[word] & bit) != 0L;
        if (satisfied == wasActive) return;
        int target = BonusMotifGraph.TARGET[motif] & 0xFF;
        int weight = BonusMotifGraph.WEIGHT[motif] & 0xFF;
        if (satisfied) {
            activeMotifs[word] |= bit;
            bonusScore[target] += weight;
        } else {
            activeMotifs[word] &= ~bit;
            bonusScore[target] -= weight;
        }
        markDirty(target);
    }

    private void removeAdjacencyForHits(long lo, long hi) {
        while (lo != 0L) {
            adjustAdjacentNeighbors(Long.numberOfTrailingZeros(lo), -1);
            lo &= lo - 1;
        }
        while (hi != 0L) {
            adjustAdjacentNeighbors(64 + Long.numberOfTrailingZeros(hi), -1);
            hi &= hi - 1;
        }
    }

    private void adjustAdjacentNeighbors(int hit, int delta) {
        int row = hit / 10;
        int col = hit % 10;
        if (row > 0) adjustAdjacent(hit - 10, delta);
        if (row < 9) adjustAdjacent(hit + 10, delta);
        if (col > 0) adjustAdjacent(hit - 1, delta);
        if (col < 9) adjustAdjacent(hit + 1, delta);
    }

    private void adjustAdjacent(int cell, int delta) {
        boolean oldAdjacent = adjacentHitCount[cell] != 0;
        adjacentHitCount[cell] += (byte) delta;
        if (oldAdjacent != (adjacentHitCount[cell] != 0)) markDirty(cell);
    }

    private void markDirty(int cell) {
        dirtyCellLo |= BitBoard100.lowMask(cell);
        dirtyCellHi |= BitBoard100.highMask(cell);
    }

    private void markDirtyMask(long lo, long hi) {
        dirtyCellLo |= lo;
        dirtyCellHi |= hi;
    }

    private void reconcileDirtyCells() {
        long lo = dirtyCellLo;
        long hi = dirtyCellHi;
        dirtyCellLo = dirtyCellHi = 0L;
        while (lo != 0L) {
            reconcileCell(Long.numberOfTrailingZeros(lo));
            lo &= lo - 1;
        }
        while (hi != 0L) {
            reconcileCell(64 + Long.numberOfTrailingZeros(hi));
            hi &= hi - 1;
        }
    }

    private void reconcileCell(int cell) {
        int oldScore = indexedScore[cell] & 0xFFFF;
        boolean oldAdjacent = indexedAdjacent[cell] != 0;
        boolean oldSelectable = indexedSelectable[cell] != 0;
        int newScore = baseScore[cell] + bonusScore[cell];
        boolean newAdjacent = adjacentHitCount[cell] != 0;
        boolean newSelectable = isUnknown(cell);
        if (newScore < MIN_TOTAL_SCORE || newScore > MAX_TOTAL_SCORE) {
            throw new IllegalStateException("Score outside verified domain: " + newScore);
        }
        if (oldScore == newScore && oldAdjacent == newAdjacent && oldSelectable == newSelectable) return;
        indexedScore[cell] = (short) newScore;
        indexedAdjacent[cell] = (byte) (newAdjacent ? 1 : 0);
        indexedSelectable[cell] = (byte) (newSelectable ? 1 : 0);
        reconcileRanking(cell, oldScore, oldAdjacent, oldSelectable,
                newScore, newAdjacent, newSelectable);
    }

    protected final boolean isUnknown(int cell) {
        return !BitBoard100.contains(shotsLo, shotsHi, cell);
    }

    final int referenceBest() {
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int cell = 0; cell < 100; cell++) {
            if (!isUnknown(cell)) continue;
            int score = baseScore[cell] + bonusScore[cell];
            if (score > bestScore) {
                best = cell;
                bestScore = score;
            } else if (score == bestScore && best != -1
                    && adjacentHitCount[cell] != 0 && adjacentHitCount[best] == 0) {
                best = cell;
            }
        }
        return best;
    }

    final void verifyRankingInvariants() {
        for (int cell = 0; cell < 100; cell++) {
            int score = baseScore[cell] + bonusScore[cell];
            boolean adjacent = adjacentHitCount[cell] != 0;
            boolean selectable = isUnknown(cell);
            if ((indexedScore[cell] & 0xFFFF) != score
                    || (indexedAdjacent[cell] != 0) != adjacent
                    || (indexedSelectable[cell] != 0) != selectable) {
                throw new IllegalStateException("indexed ranking mismatch at " + cell);
            }
        }
        verifyStructure();
        if (rankingBest() != referenceBest()) {
            throw new IllegalStateException("ranking best differs from reference scan");
        }
    }
}
