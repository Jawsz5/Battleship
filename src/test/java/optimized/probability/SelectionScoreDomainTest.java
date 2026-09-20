package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SelectionScoreDomainTest {
    @Test
    void constantsMatchBoundsDerivedFromImmutableTables() {
        int maximumBase = 0;
        for (int score : PlacementCsr.INITIAL_BASE_SCORE) maximumBase = Math.max(maximumBase, score);

        int[] allSatisfiedBonus = new int[100];
        for (int motif = 0; motif < BonusMotifGraph.MOTIF_COUNT; motif++) {
            allSatisfiedBonus[BonusMotifGraph.TARGET[motif] & 0xFF]
                    += BonusMotifGraph.WEIGHT[motif] & 0xFF;
        }
        int maximumBonus = 0;
        for (int score : allSatisfiedBonus) maximumBonus = Math.max(maximumBonus, score);

        assertEquals(34, maximumBase);
        assertEquals(220, maximumBonus);
        assertEquals(maximumBase, AbstractIncrementalSelectionStrategy.MAX_BASE_SCORE);
        assertEquals(maximumBonus, AbstractIncrementalSelectionStrategy.MAX_BONUS_SCORE);
        assertEquals(255, AbstractIncrementalSelectionStrategy.SCORE_VALUE_COUNT);
    }
}
