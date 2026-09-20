package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BonusMotifGraphTest {
    @Test
    void generatedCountsMatchTenByTenLegacyGeometry() {
        assertEquals(360, BonusMotifGraph.ADJACENCY_MOTIFS);
        assertEquals(320, BonusMotifGraph.LINE_15_MOTIFS);
        assertEquals(240, BonusMotifGraph.LINE_30_MOTIFS);
        assertEquals(920, BonusMotifGraph.MOTIF_COUNT);
        assertEquals(1_720, BonusMotifGraph.HIT_MOTIF_INCIDENCES);
        assertEquals(15, BonusMotifGraph.ACTIVE_WORD_COUNT);
        assertEquals(101, BonusMotifGraph.hitMotifOffsetSize());
        assertEquals(1_720, BonusMotifGraph.hitMotifsSize());
    }

    @Test
    void reverseCsrContainsEveryRequiredHitIncidence() {
        boolean[][] expected = new boolean[100][920];
        int count = 0;
        for (int motif = 0; motif < 920; motif++) {
            long lo = BonusMotifGraph.REQUIRED_LO[motif];
            long hi = BonusMotifGraph.REQUIRED_HI[motif];
            while (lo != 0L) {
                expected[Long.numberOfTrailingZeros(lo)][motif] = true;
                count++;
                lo &= lo - 1;
            }
            while (hi != 0L) {
                expected[64 + Long.numberOfTrailingZeros(hi)][motif] = true;
                count++;
                hi &= hi - 1;
            }
        }
        assertEquals(1_720, count);
        int reverseCount = 0;
        for (int cell = 0; cell < 100; cell++) {
            for (int at = BonusMotifGraph.HIT_MOTIF_OFFSET[cell];
                    at < BonusMotifGraph.HIT_MOTIF_OFFSET[cell + 1]; at++) {
                assertTrue(expected[cell][BonusMotifGraph.HIT_MOTIFS[at]]);
                reverseCount++;
            }
        }
        assertEquals(1_720, reverseCount);
    }
}
