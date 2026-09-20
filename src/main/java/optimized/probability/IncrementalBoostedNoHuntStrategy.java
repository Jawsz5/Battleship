package optimized.probability;

/** Incremental active-placement/CSR equivalent of frozen BoostedNoHuntProb. */
public final class IncrementalBoostedNoHuntStrategy extends AbstractBoostedNoHuntStrategy {
    private final long[] active = new long[PlacementCsr.ACTIVE_WORD_COUNT];
    private final int[] baseScore = new int[100];
    private final int[] coverage = new int[6 * 100];

    @Override
    protected void resetProbabilityState() {
        System.arraycopy(PlacementCsr.INITIAL_ACTIVE, 0, active, 0, active.length);
        System.arraycopy(PlacementCsr.INITIAL_BASE_SCORE, 0, baseScore, 0, baseScore.length);
        System.arraycopy(PlacementCsr.INITIAL_COVERAGE, 0, coverage, 0, coverage.length);
    }

    @Override
    public int chooseShot() {
        recomputeBonus();
        return selectFromBaseAndBonus(baseScore);
    }

    @Override
    protected void onMiss(int position) {
        invalidateThroughCell(position);
    }

    @Override
    protected void beforeSinkWeightChange(int length) {
        int offset = length * 100;
        for (int cell = 0; cell < 100; cell++) baseScore[cell] -= coverage[offset + cell];
    }

    @Override
    protected void onSunk(long reportedSunkLo, long reportedSunkHi) {
        long lo = reportedSunkLo;
        long hi = reportedSunkHi;
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
        for (int at = from; at < to; at++) invalidate(PlacementCsr.CELL_PLACEMENTS[at]);
    }

    private void invalidate(int placement) {
        int word = placement >>> 6;
        long bit = 1L << (placement & 63);
        if ((active[word] & bit) == 0L) return;
        active[word] &= ~bit;

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

    void verifyInvariants() {
        int[] referenceCoverage = new int[coverage.length];
        int[] referenceBase = new int[baseScore.length];
        for (int placement = 0; placement < PlacementCsr.PLACEMENT_COUNT; placement++) {
            boolean expectedActive = true;
            for (int at = PlacementCsr.PLACEMENT_CELL_OFFSET[placement];
                    at < PlacementCsr.PLACEMENT_CELL_OFFSET[placement + 1]; at++) {
                if (isBlocked(PlacementCsr.PLACEMENT_CELLS[at])) {
                    expectedActive = false;
                    break;
                }
            }
            boolean actualActive = (active[placement >>> 6] & (1L << (placement & 63))) != 0L;
            if (actualActive != expectedActive) throw new IllegalStateException("active mismatch at " + placement);
            if (!actualActive) continue;

            int length = PlacementCsr.PLACEMENT_LENGTH[placement];
            for (int at = PlacementCsr.PLACEMENT_CELL_OFFSET[placement];
                    at < PlacementCsr.PLACEMENT_CELL_OFFSET[placement + 1]; at++) {
                int cell = PlacementCsr.PLACEMENT_CELLS[at];
                referenceCoverage[length * 100 + cell]++;
                referenceBase[cell] += remain[length];
            }
        }
        for (int index = 0; index < coverage.length; index++) {
            if (coverage[index] != referenceCoverage[index]) {
                throw new IllegalStateException("coverage mismatch at " + index);
            }
        }
        for (int cell = 0; cell < 100; cell++) {
            if (baseScore[cell] != referenceBase[cell]) {
                throw new IllegalStateException("base score mismatch at " + cell);
            }
        }
    }

    int candidateScoreAt(int cell) {
        recomputeBonus();
        return baseScore[cell] + bonus[cell];
    }

    int activePlacementCount() {
        int count = 0;
        for (long word : active) count += Long.bitCount(word);
        return count;
    }

    @Override
    public String name() { return "IncrementalBoostedNoHunt"; }
}
