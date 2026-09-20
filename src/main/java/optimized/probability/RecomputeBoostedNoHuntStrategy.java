package optimized.probability;

import java.util.Arrays;
import java.util.random.RandomGenerator;
import optimized.engine.ShotResult;
import optimized.strategies.OptimizedStrategy;

/** Literal reusable byte-map/full-recomputation control on the optimized interface. */
public final class RecomputeBoostedNoHuntStrategy implements OptimizedStrategy {
    private final int[] score = new int[100];
    private final byte[] hitMap = new byte[100];
    private final int[] remain = new int[6];

    @Override
    public void reset(RandomGenerator random) {
        Arrays.fill(score, 0);
        Arrays.fill(hitMap, (byte) 0);
        Arrays.fill(remain, 0);
        remain[2] = 1;
        remain[3] = 2;
        remain[4] = 1;
        remain[5] = 1;
    }

    @Override
    public int chooseShot() {
        recompute();
        int bestCell = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int cell = 0; cell < 100; cell++) {
            if (hitMap[cell] != 0) continue;
            int candidate = score[cell];
            if (candidate > bestScore) {
                bestScore = candidate;
                bestCell = cell;
            } else if (candidate == bestScore && bestCell != -1
                    && adjacentToHit(cell) && !adjacentToHit(bestCell)) {
                bestCell = cell;
            }
        }
        if (bestCell < 0) throw new IllegalStateException("No cells left to fire at");
        return bestCell;
    }

    @Override
    public void trackShot(int position, int result, long sunkLo, long sunkHi) {
        hitMap[position] = ShotResult.isHit(result) ? (byte) 2 : (byte) 1;
        int sunkLength = ShotResult.sunkLength(result);
        if (sunkLength == 0) return;
        remain[sunkLength]--;
        while (sunkLo != 0L) {
            hitMap[Long.numberOfTrailingZeros(sunkLo)] = 3;
            sunkLo &= sunkLo - 1;
        }
        while (sunkHi != 0L) {
            hitMap[64 + Long.numberOfTrailingZeros(sunkHi)] = 3;
            sunkHi &= sunkHi - 1;
        }
    }

    private void recompute() {
        Arrays.fill(score, 0);
        for (int length = 2; length <= 5; length++) {
            int copies = remain[length];
            if (copies == 0) continue;
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col <= 10 - length; col++) {
                    int base = row * 10 + col;
                    boolean blocked = false;
                    for (int offset = 0; offset < length; offset++) {
                        byte state = hitMap[base + offset];
                        if (state == 1 || state == 3) { blocked = true; break; }
                    }
                    if (!blocked) {
                        for (int offset = 0; offset < length; offset++) {
                            int cell = base + offset;
                            if (hitMap[cell] == 0) score[cell] += copies;
                        }
                    }
                }
            }
            for (int col = 0; col < 10; col++) {
                for (int row = 0; row <= 10 - length; row++) {
                    int base = row * 10 + col;
                    boolean blocked = false;
                    for (int offset = 0; offset < length; offset++) {
                        byte state = hitMap[base + offset * 10];
                        if (state == 1 || state == 3) { blocked = true; break; }
                    }
                    if (!blocked) {
                        for (int offset = 0; offset < length; offset++) {
                            int cell = base + offset * 10;
                            if (hitMap[cell] == 0) score[cell] += copies;
                        }
                    }
                }
            }
        }

        for (int cell = 0; cell < 100; cell++) {
            if (hitMap[cell] != 2) continue;
            int row = cell / 10;
            int col = cell % 10;
            boost(row + 1, col, 10);
            boost(row - 1, col, 10);
            boost(row, col + 1, 10);
            boost(row, col - 1, 10);

            if (row > 0 && hitMap[cell - 10] == 2) {
                boost(row + 1, col, 15);
                if (row > 1 && hitMap[cell - 20] == 2) boost(row + 2, col, 30);
            }
            if (row < 9 && hitMap[cell + 10] == 2) {
                boost(row - 1, col, 15);
                if (row < 8 && hitMap[cell + 20] == 2) boost(row - 2, col, 30);
            }
            if (col > 0 && hitMap[cell - 1] == 2) {
                boost(row, col + 1, 15);
                if (col > 1 && hitMap[cell - 2] == 2) boost(row, col + 2, 30);
            }
            if (col < 9 && hitMap[cell + 1] == 2) {
                boost(row, col - 1, 15);
                if (col < 8 && hitMap[cell + 2] == 2) boost(row, col - 2, 30);
            }
        }
    }

    private void boost(int row, int col, int value) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10) return;
        int cell = row * 10 + col;
        if (hitMap[cell] == 0) score[cell] += value;
    }

    private boolean adjacentToHit(int cell) {
        int row = cell / 10;
        int col = cell % 10;
        return (row > 0 && hitMap[cell - 10] == 2)
                || (row < 9 && hitMap[cell + 10] == 2)
                || (col > 0 && hitMap[cell - 1] == 2)
                || (col < 9 && hitMap[cell + 1] == 2);
    }

    int scoreAt(int cell) {
        recompute();
        return score[cell];
    }

    @Override
    public String name() { return "RecomputeBoostedNoHunt"; }
}
