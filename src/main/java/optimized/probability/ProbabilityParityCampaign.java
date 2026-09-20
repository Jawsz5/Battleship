package optimized.probability;

import java.util.SplittableRandom;
import legacy.compstrategies.probstrats.HitMaxOptimize.BoostedNoHuntProb;
import optimized.engine.OptimizedGame;
import optimized.engine.ShotResult;

/** Deterministic common-oracle shot parity campaign for all three implementations. */
public final class ProbabilityParityCampaign {
    public record Result(int fleets, int gamesCompleted, long turnsCompared, int shotMismatches) {}

    private ProbabilityParityCampaign() {}

    public static Result run(int fleets, long seed) {
        OptimizedGame oracle = new OptimizedGame();
        RecomputeBoostedNoHuntStrategy recompute = new RecomputeBoostedNoHuntStrategy();
        IncrementalBoostedNoHuntStrategy incremental = new IncrementalBoostedNoHuntStrategy();
        SplittableRandom random = new SplittableRandom(seed);
        long turns = 0L;
        int completed = 0;

        for (int fleet = 0; fleet < fleets; fleet++) {
            oracle.resetRandom(random);
            BoostedNoHuntProb legacy = new BoostedNoHuntProb(10);
            recompute.reset(random);
            incremental.reset(random);
            int gameTurn = 0;

            while (!oracle.isComplete()) {
                int legacyShot = legacy.selectShot();
                int recomputeShot = recompute.chooseShot();
                int incrementalShot = incremental.chooseShot();
                if (legacyShot != recomputeShot || legacyShot != incrementalShot) {
                    throw new IllegalStateException("Shot mismatch: fleet=" + fleet
                            + ", turn=" + gameTurn + ", legacy=" + legacyShot
                            + ", recompute=" + recomputeShot + ", incremental=" + incrementalShot);
                }

                int result = oracle.shoot(legacyShot);
                int[] sunkCells = ShotResult.kind(result) == ShotResult.SUNK
                        ? cells(oracle.lastSunkLo(), oracle.lastSunkHi(), ShotResult.sunkLength(result))
                        : null;
                legacy.trackShot(ShotResult.isHit(result), ShotResult.sunkLength(result), legacyShot, sunkCells);
                recompute.trackShot(legacyShot, result, oracle.lastSunkLo(), oracle.lastSunkHi());
                incremental.trackShot(legacyShot, result, oracle.lastSunkLo(), oracle.lastSunkHi());
                turns++;
                gameTurn++;
            }
            completed++;
        }
        return new Result(fleets, completed, turns, 0);
    }

    public static void main(String[] args) {
        int fleets = args.length == 0 ? 10_000 : Integer.parseInt(args[0]);
        Result result = run(fleets, 0xC5_2A_2026L);
        System.out.printf("fleets=%d games_completed=%d turns_compared=%d shot_mismatches=%d%n",
                result.fleets(), result.gamesCompleted(), result.turnsCompared(), result.shotMismatches());
    }

    private static int[] cells(long lo, long hi, int length) {
        int[] cells = new int[length];
        int index = 0;
        while (lo != 0L) {
            cells[index++] = Long.numberOfTrailingZeros(lo);
            lo &= lo - 1;
        }
        while (hi != 0L) {
            cells[index++] = 64 + Long.numberOfTrailingZeros(hi);
            hi &= hi - 1;
        }
        return cells;
    }
}
