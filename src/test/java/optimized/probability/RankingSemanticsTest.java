package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RankingSemanticsTest {
    @Test
    void packedScoreAndAdjacencyKeyMatchesLiteralFrozenTieRules() {
        for (int leftScore = 0; leftScore <= AbstractIncrementalSelectionStrategy.MAX_TOTAL_SCORE;
             leftScore++) {
            for (int rightScore = 0; rightScore <= AbstractIncrementalSelectionStrategy.MAX_TOTAL_SCORE;
                 rightScore++) {
                for (boolean leftAdjacent : new boolean[] {false, true}) {
                    for (boolean rightAdjacent : new boolean[] {false, true}) {
                        for (boolean leftSelectable : new boolean[] {false, true}) {
                            for (boolean rightSelectable : new boolean[] {false, true}) {
                                assertEquals(literal(leftScore, leftAdjacent, leftSelectable,
                                                rightScore, rightAdjacent, rightSelectable),
                                        packed(leftScore, leftAdjacent, leftSelectable,
                                                rightScore, rightAdjacent, rightSelectable));
                            }
                        }
                    }
                }
            }
        }
    }

    private static int literal(int leftScore, boolean leftAdjacent, boolean leftSelectable,
                               int rightScore, boolean rightAdjacent, boolean rightSelectable) {
        int best = -1;
        if (leftSelectable) best = 0;
        if (rightSelectable && (best < 0 || rightScore > leftScore
                || (rightScore == leftScore && rightAdjacent && !leftAdjacent))) best = 1;
        return best;
    }

    private static int packed(int leftScore, boolean leftAdjacent, boolean leftSelectable,
                              int rightScore, boolean rightAdjacent, boolean rightSelectable) {
        int best = -1;
        int bestKey = Integer.MIN_VALUE;
        if (leftSelectable) {
            best = 0;
            bestKey = (leftScore << 1) | (leftAdjacent ? 1 : 0);
        }
        int rightKey = (rightScore << 1) | (rightAdjacent ? 1 : 0);
        if (rightSelectable && rightKey > bestKey) best = 1;
        return best;
    }
}
