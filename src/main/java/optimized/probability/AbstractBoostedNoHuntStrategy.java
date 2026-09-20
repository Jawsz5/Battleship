package optimized.probability;

import java.util.Arrays;
import java.util.random.RandomGenerator;
import optimized.engine.BitBoard100;
import optimized.engine.ShotResult;
import optimized.strategies.OptimizedStrategy;

/** Exact shared observation, bonus, and tie semantics of frozen BoostedNoHuntProb. */
abstract class AbstractBoostedNoHuntStrategy implements OptimizedStrategy {
    protected static final int ADJ = 10;
    protected static final int LINE = 15;

    protected final int[] remain = new int[6];
    protected final int[] bonus = new int[100];
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
        shotsLo = shotsHi = hitsLo = hitsHi = sunkLo = sunkHi = 0L;
        resetProbabilityState();
    }

    protected abstract void resetProbabilityState();

    @Override
    public void trackShot(int position, int result, long reportedSunkLo, long reportedSunkHi) {
        long cellLo = BitBoard100.lowMask(position);
        long cellHi = BitBoard100.highMask(position);
        shotsLo |= cellLo;
        shotsHi |= cellHi;

        int kind = ShotResult.kind(result);
        if (kind == ShotResult.MISS) {
            onMiss(position);
            return;
        }

        hitsLo |= cellLo;
        hitsHi |= cellHi;
        if (kind == ShotResult.SUNK) {
            int length = ShotResult.sunkLength(result);
            beforeSinkWeightChange(length);
            remain[length]--;
            hitsLo &= ~reportedSunkLo;
            hitsHi &= ~reportedSunkHi;
            sunkLo |= reportedSunkLo;
            sunkHi |= reportedSunkHi;
            shotsLo |= reportedSunkLo;
            shotsHi |= reportedSunkHi;
            onSunk(reportedSunkLo, reportedSunkHi);
        }
    }

    protected void onMiss(int position) {}
    protected void beforeSinkWeightChange(int length) {}
    protected void onSunk(long reportedSunkLo, long reportedSunkHi) {}

    protected final boolean isUnknown(int cell) {
        return !BitBoard100.contains(shotsLo, shotsHi, cell);
    }

    protected final boolean isHit(int cell) {
        return BitBoard100.contains(hitsLo, hitsHi, cell);
    }

    protected final boolean isBlocked(int cell) {
        if (isUnknown(cell) || isHit(cell)) return false;
        return true;
    }

    protected final boolean adjacentToHit(int cell) {
        int row = cell / 10;
        int col = cell % 10;
        return (row > 0 && isHit(cell - 10))
                || (row < 9 && isHit(cell + 10))
                || (col > 0 && isHit(cell - 1))
                || (col < 9 && isHit(cell + 1));
    }

    protected final void recomputeBonus() {
        Arrays.fill(bonus, 0);
        for (int cell = 0; cell < 100; cell++) {
            if (!isHit(cell)) continue;
            int row = cell / 10;
            int col = cell % 10;

            boost(row + 1, col, ADJ);
            boost(row - 1, col, ADJ);
            boost(row, col + 1, ADJ);
            boost(row, col - 1, ADJ);

            if (row > 0 && isHit(cell - 10)) {
                boost(row + 1, col, LINE);
                if (row > 1 && isHit(cell - 20)) boost(row + 2, col, LINE * 2);
            }
            if (row < 9 && isHit(cell + 10)) {
                boost(row - 1, col, LINE);
                if (row < 8 && isHit(cell + 20)) boost(row - 2, col, LINE * 2);
            }
            if (col > 0 && isHit(cell - 1)) {
                boost(row, col + 1, LINE);
                if (col > 1 && isHit(cell - 2)) boost(row, col + 2, LINE * 2);
            }
            if (col < 9 && isHit(cell + 1)) {
                boost(row, col - 1, LINE);
                if (col < 8 && isHit(cell + 2)) boost(row, col - 2, LINE * 2);
            }
        }
    }

    private void boost(int row, int col, int value) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10) return;
        int cell = row * 10 + col;
        if (isUnknown(cell)) bonus[cell] += value;
    }

    protected final int selectFromScores(int[] scores) {
        int bestCell = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int cell = 0; cell < 100; cell++) {
            if (!isUnknown(cell)) continue;
            int score = scores[cell];
            if (score > bestScore) {
                bestScore = score;
                bestCell = cell;
            } else if (score == bestScore && bestCell != -1
                    && adjacentToHit(cell) && !adjacentToHit(bestCell)) {
                bestCell = cell;
            }
        }
        if (bestCell < 0) throw new IllegalStateException("No cells left to fire at");
        return bestCell;
    }

    protected final int selectFromBaseAndBonus(int[] baseScore) {
        int bestCell = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int cell = 0; cell < 100; cell++) {
            if (!isUnknown(cell)) continue;
            int score = baseScore[cell] + bonus[cell];
            if (score > bestScore) {
                bestScore = score;
                bestCell = cell;
            } else if (score == bestScore && bestCell != -1
                    && adjacentToHit(cell) && !adjacentToHit(bestCell)) {
                bestCell = cell;
            }
        }
        if (bestCell < 0) throw new IllegalStateException("No cells left to fire at");
        return bestCell;
    }
}
