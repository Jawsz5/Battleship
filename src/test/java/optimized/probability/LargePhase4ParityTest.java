package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LargePhase4ParityTest {
    @Test
    void tenThousandFleetsMatchEveryFrozenImplementationExactly() {
        Phase4ParityCampaign.Result result = Phase4ParityCampaign.run(10_000, 0xB0_4A_2026L);
        assertEquals(10_000, result.gamesCompleted());
        assertEquals(0, result.shotMismatches());
    }
}
