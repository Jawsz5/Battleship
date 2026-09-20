# Phase 1 benchmarks

Run the single-thread warm-up/trial benchmark with:

```shell
./gradlew phase1Benchmark
```

The harness compares legacy and optimized Random, Parity, and RandomHunt simulations, reporting the median-time trial with game throughput and turn statistics. It also characterizes optimized fleet generation, fixed-fleet reset, game-over checks, ship-sunk checks, overlap checks, and shot resolution.

Workload controls are JVM system properties: `benchmark.warmupGames`, `benchmark.games`, and `benchmark.trials`.

Phase 2 exact probability parity and benchmarks run with:

```shell
./gradlew phase2Parity
./gradlew phase2Benchmark
```

Phase 2 uses identical deterministic fleet corpora for legacy, optimized-recompute, and optimized-incremental comparisons. Its controls are `phase2.parityFleets`, `phase2.warmupGames`, `phase2.trials`, `phase2.largeGames`, `phase2.millionGames`, and `phase2.microOperations`.

Phase 3 verification and A/B/C/D measurement run with:

```shell
./gradlew phase3Parity
./gradlew phase3Benchmark
```

Phase 3 controls are `phase3.parityFleets`, `phase3.warmupGames`, `phase3.trials`, `phase3.largeGames`, `phase3.millionGames`, and `phase3.microOperations`. The `phase3.onlyD` and `phase3.profileGames` controls support focused profiling of the Phase 3 implementation.

Phase 4 verification and selection benchmarking run with:

```shell
./gradlew phase4Parity -Dphase4.parityFleets=100000
./gradlew phase4Benchmark
```

Phase 4 compares the Phase 3 linear scan, packed score buckets, and a fixed tournament tree. Controls are `phase4.parityFleets`, `phase4.warmupGames`, `phase4.trials`, `phase4.largeGames`, `phase4.millionGames`, and `phase4.microOperations`. Use `phase4.only=scan`, `bucket`, or `tournament` with `phase4.profileGames` for a focused measurement.

Phase 5 fixed-worker scaling runs with:

```shell
./gradlew phase5Benchmark
```

It measures a deterministic fixed-corpus mode and worker-local random-fleet mode. Controls are `phase5.warmupGames`, `phase5.corpusGames`, `phase5.randomGames`, `phase5.trials`, and `phase5.workers` (a comma-separated worker-count list).

Phase 6 is the final engine-performance experiment:

```shell
./gradlew phase6Benchmark
```

It checks the Java Vector API proof-of-concept and lane-refilling scalar batch widths. Controls are `phase6.games`, `phase6.trials`, `phase6.widths`, `phase6.vectorOperations`, `phase6.singleWorker`, and `phase6.parallelWorkers`.
