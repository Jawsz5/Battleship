package optimized.probability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SplittableRandom;
import legacy.compstrategies.probstrats.HitMaxOptimize.BoostedNoHuntProb;
import legacy.gamefiles.Ocean;
import legacy.gamefiles.Ship;
import optimized.engine.BitBoard100;
import optimized.engine.OptimizedGame;
import optimized.engine.ShipPlacements;
import optimized.engine.ShotResult;
import optimized.strategies.OptimizedStrategy;

/** Single-thread A/B/C benchmark using identical deterministic fleet corpora. */
public final class Phase2ProbabilityBenchmark {
    private static final String[] SHIP_NAMES = {
            "aircraftcarrier", "battleship", "destroyer", "cruiser", "submarine"
    };
    private static volatile long blackhole;

    record Result(int games, long elapsedNanos, long totalTurns,
                          double mean, double median, int min, int max) {
        double gamesPerSecond() { return games * 1_000_000_000.0 / elapsedNanos; }
    }

    record FleetCorpus(int games, long[] low, long[] high) {}

    public static void main(String[] args) throws Exception {
        int warmupGames = Integer.getInteger("phase2.warmupGames", 2_000);
        int trials = Integer.getInteger("phase2.trials", 3);
        int largeGames = Integer.getInteger("phase2.largeGames", 100_000);
        int millionGames = Integer.getInteger("phase2.millionGames", 1_000_000);

        FleetCorpus warmup = generateCorpus(warmupGames, 0xA11CE5L);
        runLegacy(warmup);
        runOptimized(warmup, new RecomputeBoostedNoHuntStrategy());
        runOptimized(warmup, new IncrementalBoostedNoHuntStrategy());

        System.out.printf("Phase 2 single-thread benchmark: warmup=%d, trials=%d%n", warmupGames, trials);
        System.out.println("scale,implementation,games,time_ms,games_per_sec,total_turns,mean,median,min,max");
        runComparableScale(generateCorpus(10_000, 0x10_000L), trials);
        runComparableScale(generateCorpus(largeGames, 0x100_000L), trials);

        FleetCorpus million = generateCorpus(millionGames, 0x1_000_000L);
        Result[] incrementalResults = new Result[trials];
        for (int trial = 0; trial < trials; trial++) {
            incrementalResults[trial] = runOptimized(million, new IncrementalBoostedNoHuntStrategy());
        }
        print("incremental-scale", "C-optimized-incremental", medianTrial(incrementalResults));
        benchmarkProbabilityOperations();
    }

    private static void runComparableScale(FleetCorpus corpus, int trials) throws Exception {
        Result[] legacy = new Result[trials];
        Result[] recompute = new Result[trials];
        Result[] incremental = new Result[trials];
        for (int trial = 0; trial < trials; trial++) {
            legacy[trial] = runLegacy(corpus);
            recompute[trial] = runOptimized(corpus, new RecomputeBoostedNoHuntStrategy());
            incremental[trial] = runOptimized(corpus, new IncrementalBoostedNoHuntStrategy());
        }
        String scale = corpus.games == 10_000 ? "10k" : "100k";
        print(scale, "A-legacy", medianTrial(legacy));
        print(scale, "B-optimized-recompute", medianTrial(recompute));
        print(scale, "C-optimized-incremental", medianTrial(incremental));
    }

    static Result runLegacy(FleetCorpus corpus) throws Exception {
        int[] turns = new int[corpus.games];
        long total = 0L;
        int min = 100;
        int max = 0;
        long start = System.nanoTime();
        for (int gameIndex = 0; gameIndex < corpus.games; gameIndex++) {
            Ocean ocean = loadLegacyOcean(corpus, gameIndex);
            ArrayList<Ship> ships = ocean.getBoats();
            boolean[] reported = new boolean[5];
            BoostedNoHuntProb strategy = new BoostedNoHuntProb(10);
            int turn = 0;
            while (!ocean.isAllSunk()) {
                int shot = strategy.selectShot();
                boolean hit = ocean.isHit(shot);
                if (hit) for (Ship ship : ships) ship.hit(shot);
                int sunkLength = 0;
                int[] sunkCells = null;
                for (int ship = 0; ship < 5; ship++) {
                    if (!reported[ship] && ships.get(ship).isSunk()) {
                        reported[ship] = true;
                        sunkLength = ships.get(ship).getBoatLength();
                        sunkCells = ships.get(ship).getSaveSpots();
                        break;
                    }
                }
                strategy.trackShot(hit, sunkLength, shot, sunkCells);
                turn++;
            }
            turns[gameIndex] = turn;
            total += turn;
            if (turn < min) min = turn;
            if (turn > max) max = turn;
        }
        return finish(corpus.games, System.nanoTime() - start, total, turns, min, max);
    }

    static Result runOptimized(FleetCorpus corpus, OptimizedStrategy strategy) {
        int[] turns = new int[corpus.games];
        long total = 0L;
        int min = 100;
        int max = 0;
        long[] fleetLo = new long[5];
        long[] fleetHi = new long[5];
        OptimizedGame game = new OptimizedGame();
        SplittableRandom unusedDeterministicRng = new SplittableRandom(1);
        long start = System.nanoTime();
        for (int gameIndex = 0; gameIndex < corpus.games; gameIndex++) {
            System.arraycopy(corpus.low, gameIndex * 5, fleetLo, 0, 5);
            System.arraycopy(corpus.high, gameIndex * 5, fleetHi, 0, 5);
            game.resetWithFleet(fleetLo, fleetHi);
            strategy.reset(unusedDeterministicRng);
            int turn = 0;
            while (!game.isComplete()) {
                int shot = strategy.chooseShot();
                int result = game.shoot(shot);
                strategy.trackShot(shot, result, game.lastSunkLo(), game.lastSunkHi());
                turn++;
            }
            turns[gameIndex] = turn;
            total += turn;
            if (turn < min) min = turn;
            if (turn > max) max = turn;
        }
        return finish(corpus.games, System.nanoTime() - start, total, turns, min, max);
    }

    private static Result finish(int games, long elapsed, long total, int[] turns, int min, int max) {
        Arrays.sort(turns);
        double median = (games & 1) == 0
                ? (turns[games / 2 - 1] + turns[games / 2]) / 2.0
                : turns[games / 2];
        return new Result(games, elapsed, total, (double) total / games, median, min, max);
    }

    static Result medianTrial(Result[] results) {
        Arrays.sort(results, Comparator.comparingLong(Result::elapsedNanos));
        return results[results.length / 2];
    }

    static void print(String scale, String implementation, Result result) {
        System.out.printf("%s,%s,%d,%.3f,%.1f,%d,%.5f,%.1f,%d,%d%n",
                scale, implementation, result.games, result.elapsedNanos / 1_000_000.0,
                result.gamesPerSecond(), result.totalTurns, result.mean, result.median,
                result.min, result.max);
    }

    static FleetCorpus generateCorpus(int games, long seed) {
        long[] low = new long[games * 5];
        long[] high = new long[games * 5];
        SplittableRandom random = new SplittableRandom(seed);
        for (int game = 0; game < games; game++) {
            long occupiedLo = 0L;
            long occupiedHi = 0L;
            for (int ship = 0; ship < 5; ship++) {
                int length = OptimizedGame.SHIP_LENGTHS[ship];
                while (true) {
                    int placement = random.nextInt(ShipPlacements.countForLength(length));
                    long candidateLo = ShipPlacements.lowMask(length, placement);
                    long candidateHi = ShipPlacements.highMask(length, placement);
                    if (((occupiedLo & candidateLo) | (occupiedHi & candidateHi)) != 0L) continue;
                    low[game * 5 + ship] = candidateLo;
                    high[game * 5 + ship] = candidateHi;
                    occupiedLo |= candidateLo;
                    occupiedHi |= candidateHi;
                    break;
                }
            }
        }
        return new FleetCorpus(games, low, high);
    }

    private static Ocean loadLegacyOcean(FleetCorpus corpus, int game) throws Exception {
        Ocean ocean = new Ocean(10);
        for (int ship = 0; ship < 5; ship++) {
            long lo = corpus.low[game * 5 + ship];
            long hi = corpus.high[game * 5 + ship];
            int first = firstCell(lo, hi);
            int second = secondCell(lo, hi, first);
            ocean.addBoats(first, second - first == 10, SHIP_NAMES[ship]);
        }
        return ocean;
    }

    private static int firstCell(long lo, long hi) {
        return lo != 0L ? Long.numberOfTrailingZeros(lo) : 64 + Long.numberOfTrailingZeros(hi);
    }

    private static int secondCell(long lo, long hi, int first) {
        if (first < 64) lo &= ~(1L << first);
        else hi &= ~(1L << (first - 64));
        return firstCell(lo, hi);
    }

    private static void benchmarkProbabilityOperations() {
        int operations = Integer.getInteger("phase2.microOperations", 200_000);
        SplittableRandom random = new SplittableRandom(9);
        RecomputeBoostedNoHuntStrategy recompute = new RecomputeBoostedNoHuntStrategy();
        IncrementalBoostedNoHuntStrategy incremental = new IncrementalBoostedNoHuntStrategy();

        long start = System.nanoTime();
        for (int i = 0; i < operations; i++) incremental.reset(random);
        line("incremental_reset", operations, System.nanoTime() - start);

        start = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            incremental.reset(random);
            incremental.trackShot(44, ShotResult.HIT, 0L, 0L);
            blackhole ^= incremental.hitsLo;
        }
        line("reset_plus_hit_update", operations, System.nanoTime() - start);

        start = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            incremental.reset(random);
            incremental.trackShot(44, ShotResult.MISS, 0L, 0L);
            blackhole ^= incremental.activePlacementCount();
        }
        line("reset_plus_miss_invalidation", operations, System.nanoTime() - start);

        long sinkLo = BitBoard100.lowMask(43) | BitBoard100.lowMask(44) | BitBoard100.lowMask(45);
        start = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            incremental.reset(random);
            incremental.trackShot(45, ShotResult.sunk(3), sinkLo, 0L);
            blackhole ^= incremental.activePlacementCount();
        }
        line("reset_plus_sunk_update", operations, System.nanoTime() - start);

        recompute.reset(random);
        start = System.nanoTime();
        for (int i = 0; i < operations; i++) blackhole ^= recompute.chooseShot();
        line("full_recompute_select", operations, System.nanoTime() - start);

        incremental.reset(random);
        start = System.nanoTime();
        for (int i = 0; i < operations; i++) blackhole ^= incremental.chooseShot();
        line("incremental_select", operations, System.nanoTime() - start);
    }

    private static void line(String operation, long count, long nanos) {
        System.out.printf("micro,%s,%d,%.3f,%.3f%n", operation, count,
                nanos / 1_000_000.0, (double) nanos / count);
    }
}
