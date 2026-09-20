package optimized.probability;

import java.util.SplittableRandom;
import legacy.compstrategies.probstrats.HitMaxOptimize.BoostedNoHuntProb;
import optimized.engine.OptimizedGame;
import optimized.engine.ShotResult;

/** Common-oracle exact parity across the frozen implementations and both Phase 4 candidates. */
public final class Phase4ParityCampaign {
    public record Result(int fleets, int gamesCompleted, long turnsCompared, int shotMismatches) {}

    private Phase4ParityCampaign() {}

    public static Result run(int fleets, long seed) {
        OptimizedGame oracle = new OptimizedGame();
        RecomputeBoostedNoHuntStrategy phase1 = new RecomputeBoostedNoHuntStrategy();
        IncrementalBoostedNoHuntStrategy phase2 = new IncrementalBoostedNoHuntStrategy();
        IncrementalBonusBoostedNoHuntStrategy phase3 = new IncrementalBonusBoostedNoHuntStrategy();
        BucketSelectionBoostedNoHuntStrategy buckets = new BucketSelectionBoostedNoHuntStrategy();
        TournamentSelectionBoostedNoHuntStrategy tournament = new TournamentSelectionBoostedNoHuntStrategy();
        SplittableRandom random = new SplittableRandom(seed);
        long turns = 0L;

        for (int fleet = 0; fleet < fleets; fleet++) {
            oracle.resetRandom(random);
            BoostedNoHuntProb legacy = new BoostedNoHuntProb(10);
            phase1.reset(random);
            phase2.reset(random);
            phase3.reset(random);
            buckets.reset(random);
            tournament.reset(random);
            int gameTurn = 0;
            while (!oracle.isComplete()) {
                int legacyShot = legacy.selectShot();
                int phase1Shot = phase1.chooseShot();
                int phase2Shot = phase2.chooseShot();
                int phase3Shot = phase3.chooseShot();
                int bucketShot = buckets.chooseShot();
                int tournamentShot = tournament.chooseShot();
                if (legacyShot != phase1Shot || legacyShot != phase2Shot
                        || legacyShot != phase3Shot || legacyShot != bucketShot
                        || legacyShot != tournamentShot) {
                    throw new IllegalStateException("Shot mismatch: fleet=" + fleet
                            + ", turn=" + gameTurn + ", legacy=" + legacyShot
                            + ", phase1=" + phase1Shot + ", phase2=" + phase2Shot
                            + ", phase3=" + phase3Shot + ", buckets=" + bucketShot
                            + ", tournament=" + tournamentShot);
                }
                int result = oracle.shoot(legacyShot);
                int[] sunkCells = ShotResult.kind(result) == ShotResult.SUNK
                        ? cells(oracle.lastSunkLo(), oracle.lastSunkHi(), ShotResult.sunkLength(result))
                        : null;
                legacy.trackShot(ShotResult.isHit(result), ShotResult.sunkLength(result), legacyShot, sunkCells);
                phase1.trackShot(legacyShot, result, oracle.lastSunkLo(), oracle.lastSunkHi());
                phase2.trackShot(legacyShot, result, oracle.lastSunkLo(), oracle.lastSunkHi());
                phase3.trackShot(legacyShot, result, oracle.lastSunkLo(), oracle.lastSunkHi());
                buckets.trackShot(legacyShot, result, oracle.lastSunkLo(), oracle.lastSunkHi());
                tournament.trackShot(legacyShot, result, oracle.lastSunkLo(), oracle.lastSunkHi());
                turns++;
                gameTurn++;
            }
        }
        return new Result(fleets, fleets, turns, 0);
    }

    public static void main(String[] args) {
        int fleets = args.length == 0 ? 100_000 : Integer.parseInt(args[0]);
        Result result = run(fleets, 0xB0_4A_2026L);
        System.out.printf("fleets=%d games_completed=%d turns_compared=%d shot_mismatches=%d%n",
                result.fleets(), result.gamesCompleted(), result.turnsCompared(), result.shotMismatches());
    }

    private static int[] cells(long lo, long hi, int length) {
        int[] result = new int[length];
        int index = 0;
        while (lo != 0L) {
            result[index++] = Long.numberOfTrailingZeros(lo);
            lo &= lo - 1;
        }
        while (hi != 0L) {
            result[index++] = 64 + Long.numberOfTrailingZeros(hi);
            hi &= hi - 1;
        }
        return result;
    }
}
