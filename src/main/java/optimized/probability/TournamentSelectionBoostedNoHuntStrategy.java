package optimized.probability;

import java.util.Arrays;

/** Fixed 128-leaf tournament tree using the exact frozen three-level ranking key. */
public final class TournamentSelectionBoostedNoHuntStrategy extends AbstractIncrementalSelectionStrategy {
    private static final int LEAF_BASE = 128;
    private final int[] winner = new int[256];

    @Override
    protected void resetRankingStructure() {
        Arrays.fill(winner, -1);
        for (int cell = 0; cell < 100; cell++) winner[LEAF_BASE + cell] = cell;
        for (int node = LEAF_BASE - 1; node > 0; node--) {
            winner[node] = better(winner[node << 1], winner[(node << 1) + 1]);
        }
    }

    @Override
    protected void reconcileRanking(int cell, int oldScore, boolean oldAdjacent,
                                    boolean oldSelectable, int newScore,
                                    boolean newAdjacent, boolean newSelectable) {
        int node = LEAF_BASE + cell;
        winner[node] = newSelectable ? cell : -1;
        node >>>= 1;
        while (node > 0) {
            int next = better(winner[node << 1], winner[(node << 1) + 1]);
            winner[node] = next;
            node >>>= 1;
        }
    }

    @Override
    protected int rankingBest() { return winner[1]; }

    private int better(int left, int right) {
        if (left < 0) return right;
        if (right < 0) return left;
        int leftScore = indexedScore[left] & 0xFFFF;
        int rightScore = indexedScore[right] & 0xFFFF;
        if (leftScore != rightScore) return leftScore > rightScore ? left : right;
        boolean leftAdjacent = indexedAdjacent[left] != 0;
        boolean rightAdjacent = indexedAdjacent[right] != 0;
        if (leftAdjacent != rightAdjacent) return leftAdjacent ? left : right;
        return Math.min(left, right);
    }

    @Override
    protected void verifyStructure() {
        for (int cell = 0; cell < 100; cell++) {
            int expected = indexedSelectable[cell] != 0 ? cell : -1;
            if (winner[LEAF_BASE + cell] != expected) {
                throw new IllegalStateException("tournament leaf mismatch at " + cell);
            }
        }
        for (int node = LEAF_BASE - 1; node > 0; node--) {
            int expected = better(winner[node << 1], winner[(node << 1) + 1]);
            if (winner[node] != expected) throw new IllegalStateException("tournament node mismatch at " + node);
        }
    }

    @Override
    public String name() { return "TournamentSelectionBoostedNoHunt"; }
}
