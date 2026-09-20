package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LargePhase3ParityTest {
    @Test
    void tenThousandFleetsMatchFrozenAndPhase2Exactly() {
        Phase3ParityCampaign.Result result = Phase3ParityCampaign.run(10_000, 0xB0_3A_2026L);
        assertEquals(10_000, result.gamesCompleted());
        assertEquals(0, result.shotMismatches());
    }
}
