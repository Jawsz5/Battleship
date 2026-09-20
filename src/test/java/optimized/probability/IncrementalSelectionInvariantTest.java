package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import optimized.engine.OptimizedGame;
import optimized.engine.ShotResult;

class IncrementalSelectionInvariantTest {
    @Test
    void targetedMissHitAndSinkTransitionsKeepBothIndexesExact() {
        BucketSelectionBoostedNoHuntStrategy buckets = new BucketSelectionBoostedNoHuntStrategy();
        TournamentSelectionBoostedNoHuntStrategy tournament = new TournamentSelectionBoostedNoHuntStrategy();
        IncrementalBonusBoostedNoHuntStrategy phase3 = new IncrementalBonusBoostedNoHuntStrategy();
        SplittableRandom random = new SplittableRandom(4);
        buckets.reset(random);
        tournament.reset(random);
        phase3.reset(random);
        verify(buckets, tournament, phase3);

        observe(buckets, tournament, phase3, 44, ShotResult.MISS, 0L, 0L);
        observe(buckets, tournament, phase3, 0, ShotResult.HIT, 0L, 0L);
        observe(buckets, tournament, phase3, 1, ShotResult.HIT, 0L, 0L);
        observe(buckets, tournament, phase3, 2, ShotResult.HIT, 0L, 0L);
        long sunkLo = (1L << 0) | (1L << 1) | (1L << 2);
        observe(buckets, tournament, phase3, 3, ShotResult.sunk(4), sunkLo | (1L << 3), 0L);
        observe(buckets, tournament, phase3, 63, ShotResult.HIT, 0L, 0L);
        observe(buckets, tournament, phase3, 64, ShotResult.MISS, 0L, 0L);
    }

    @Test
    void completeGamesMatchPhase3AfterEveryTransition() {
        SplittableRandom random = new SplittableRandom(0x51EC7L);
        OptimizedGame game = new OptimizedGame();
        BucketSelectionBoostedNoHuntStrategy buckets = new BucketSelectionBoostedNoHuntStrategy();
        TournamentSelectionBoostedNoHuntStrategy tournament = new TournamentSelectionBoostedNoHuntStrategy();
        IncrementalBonusBoostedNoHuntStrategy phase3 = new IncrementalBonusBoostedNoHuntStrategy();
        for (int fleet = 0; fleet < 500; fleet++) {
            game.resetRandom(random);
            buckets.reset(random);
            tournament.reset(random);
            phase3.reset(random);
            verify(buckets, tournament, phase3);
            while (!game.isComplete()) {
                int shot = phase3.chooseShot();
                int result = game.shoot(shot);
                observe(buckets, tournament, phase3, shot, result,
                        game.lastSunkLo(), game.lastSunkHi());
            }
        }
    }

    @Test
    void randomizedValidObservationsExerciseNonWinningCandidateUpdates() {
        SplittableRandom random = new SplittableRandom(0xD1A7L);
        OptimizedGame game = new OptimizedGame();
        BucketSelectionBoostedNoHuntStrategy buckets = new BucketSelectionBoostedNoHuntStrategy();
        TournamentSelectionBoostedNoHuntStrategy tournament = new TournamentSelectionBoostedNoHuntStrategy();
        IncrementalBonusBoostedNoHuntStrategy phase3 = new IncrementalBonusBoostedNoHuntStrategy();
        int[] available = new int[100];
        for (int fleet = 0; fleet < 500; fleet++) {
            game.resetRandom(random);
            buckets.reset(random);
            tournament.reset(random);
            phase3.reset(random);
            for (int cell = 0; cell < 100; cell++) available[cell] = cell;
            int count = 100;
            while (!game.isComplete()) {
                int selected = random.nextInt(count);
                int shot = available[selected];
                available[selected] = available[--count];
                int result = game.shoot(shot);
                observe(buckets, tournament, phase3, shot, result,
                        game.lastSunkLo(), game.lastSunkHi());
            }
        }
    }

    private static void observe(BucketSelectionBoostedNoHuntStrategy buckets,
                                TournamentSelectionBoostedNoHuntStrategy tournament,
                                IncrementalBonusBoostedNoHuntStrategy phase3,
                                int shot, int result, long sunkLo, long sunkHi) {
        buckets.trackShot(shot, result, sunkLo, sunkHi);
        tournament.trackShot(shot, result, sunkLo, sunkHi);
        phase3.trackShot(shot, result, sunkLo, sunkHi);
        verify(buckets, tournament, phase3);
    }

    private static void verify(BucketSelectionBoostedNoHuntStrategy buckets,
                               TournamentSelectionBoostedNoHuntStrategy tournament,
                               IncrementalBonusBoostedNoHuntStrategy phase3) {
        buckets.verifyRankingInvariants();
        tournament.verifyRankingInvariants();
        if (buckets.referenceBest() < 0) {
            assertEquals(-1, tournament.referenceBest());
            return;
        }
        assertEquals(phase3.chooseShot(), buckets.chooseShot());
        assertEquals(phase3.chooseShot(), tournament.chooseShot());
    }
}
