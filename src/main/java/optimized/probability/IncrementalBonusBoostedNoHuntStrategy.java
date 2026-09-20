package optimized.probability;

import java.util.Arrays;
import java.util.random.RandomGenerator;
import optimized.engine.BitBoard100;
import optimized.engine.ShotResult;
import optimized.strategies.OptimizedStrategy;

/** Phase 3: Phase 2 placement CSR plus incrementally maintained legacy bonus motifs. */
public final class IncrementalBonusBoostedNoHuntStrategy implements OptimizedStrategy {
    private final int[] remain = new int[6];
    private final long[] activePlacements = new long[PlacementCsr.ACTIVE_WORD_COUNT];
    private final int[] baseScore = new int[100];
    private final int[] coverage = new int[600];

    private final long[] activeMotifs = new long[BonusMotifGraph.ACTIVE_WORD_COUNT];
    private final long[] dirtyMotifs = new long[BonusMotifGraph.ACTIVE_WORD_COUNT];
    private final int[] bonusScore = new int[100];
    private final byte[] adjacentHitCount = new byte[100];

    private long shotsLo;
    private long shotsHi;
    private long hitsLo;
    private long hitsHi;
    private long sunkLo;
    private long sunkHi;

    @Override
    public void reset(RandomGenerator random) {
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
    }

    @Override
    public int chooseShot() {
        int bestCell = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int cell = 0; cell < 100; cell++) {
            if (!isUnknown(cell)) continue;
            int score = baseScore[cell] + bonusScore[cell];
            if (score > bestScore) {
                bestScore = score;
                bestCell = cell;
            } else if (score == bestScore && bestCell != -1
                    && adjacentHitCount[cell] != 0 && adjacentHitCount[bestCell] == 0) {
                bestCell = cell;
            }
        }
        if (bestCell < 0) throw new IllegalStateException("No cells left to fire at");
        return bestCell;
    }

    @Override
    public void trackShot(int position, int result, long reportedSunkLo, long reportedSunkHi) {
        long cellLo = BitBoard100.lowMask(position);
        long cellHi = BitBoard100.highMask(position);
        shotsLo |= cellLo;
        shotsHi |= cellHi;
        int kind = ShotResult.kind(result);

        if (kind == ShotResult.MISS) {
            invalidateThroughCell(position);
            return; // MISS cannot change any bonus motif or adjacency count.
        }

        if (kind == ShotResult.HIT) {
            hitsLo |= cellLo;
            hitsHi |= cellHi;
            adjustAdjacentNeighbors(position, 1);
            reevaluateIncidentMotifs(cellLo, cellHi);
            return;
        }

        int length = ShotResult.sunkLength(result);
        int coverageOffset = length * 100;
        for (int cell = 0; cell < 100; cell++) baseScore[cell] -= coverage[coverageOffset + cell];
        remain[length]--;

        // Only cells that were unsunk hits before this atomic sink transition have active effects to remove.
        long removedHitLo = hitsLo & reportedSunkLo;
        long removedHitHi = hitsHi & reportedSunkHi;
        removeAdjacencyForHits(removedHitLo, removedHitHi);
        hitsLo &= ~reportedSunkLo;
        hitsHi &= ~reportedSunkHi;
        sunkLo |= reportedSunkLo;
        sunkHi |= reportedSunkHi;
        shotsLo |= reportedSunkLo;
        shotsHi |= reportedSunkHi;
        reevaluateIncidentMotifs(removedHitLo, removedHitHi);

        invalidateThroughMask(reportedSunkLo, reportedSunkHi);
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
                int bitIndex = Long.numberOfTrailingZeros(dirty);
                int motif = (word << 6) + bitIndex;
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
        if (row > 0) adjacentHitCount[hit - 10] += (byte) delta;
        if (row < 9) adjacentHitCount[hit + 10] += (byte) delta;
        if (col > 0) adjacentHitCount[hit - 1] += (byte) delta;
        if (col < 9) adjacentHitCount[hit + 1] += (byte) delta;
    }

    private boolean isUnknown(int cell) {
        return !BitBoard100.contains(shotsLo, shotsHi, cell);
    }

    void verifyBonusInvariants() {
        int[] referenceBonus = new int[100];
        for (int motif = 0; motif < BonusMotifGraph.MOTIF_COUNT; motif++) {
            boolean satisfied = (hitsLo & BonusMotifGraph.REQUIRED_LO[motif]) == BonusMotifGraph.REQUIRED_LO[motif]
                    && (hitsHi & BonusMotifGraph.REQUIRED_HI[motif]) == BonusMotifGraph.REQUIRED_HI[motif];
            boolean active = (activeMotifs[motif >>> 6] & (1L << (motif & 63))) != 0L;
            if (active != satisfied) throw new IllegalStateException("motif mismatch at " + motif);
            if (active) referenceBonus[BonusMotifGraph.TARGET[motif] & 0xFF]
                    += BonusMotifGraph.WEIGHT[motif] & 0xFF;
        }
        for (int cell = 0; cell < 100; cell++) {
            if (bonusScore[cell] != referenceBonus[cell]) {
                throw new IllegalStateException("bonus mismatch at " + cell);
            }
            int adjacent = 0;
            int row = cell / 10;
            int col = cell % 10;
            if (row > 0 && BitBoard100.contains(hitsLo, hitsHi, cell - 10)) adjacent++;
            if (row < 9 && BitBoard100.contains(hitsLo, hitsHi, cell + 10)) adjacent++;
            if (col > 0 && BitBoard100.contains(hitsLo, hitsHi, cell - 1)) adjacent++;
            if (col < 9 && BitBoard100.contains(hitsLo, hitsHi, cell + 1)) adjacent++;
            if ((adjacentHitCount[cell] & 0xFF) != adjacent) {
                throw new IllegalStateException("adjacency mismatch at " + cell);
            }
        }
    }

    int bonusScoreAt(int cell) { return bonusScore[cell]; }
    int adjacentHitCountAt(int cell) { return adjacentHitCount[cell] & 0xFF; }
    long shotsLo() { return shotsLo; }
    long shotsHi() { return shotsHi; }
    long hitsLo() { return hitsLo; }
    long hitsHi() { return hitsHi; }

    @Override
    public String name() { return "IncrementalBonusBoostedNoHunt"; }
}
