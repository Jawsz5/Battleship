package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LargeProbabilityParityTest {
    @Test
    void tenThousandDeterministicFleetsHaveExactShotParity() {
        ProbabilityParityCampaign.Result result = ProbabilityParityCampaign.run(10_000, 0xC5_2A_2026L);
        assertEquals(10_000, result.fleets());
        assertEquals(10_000, result.gamesCompleted());
        assertEquals(0, result.shotMismatches());
    }
}
