# Battleship

This project implements Battleship simulation strategies. The repository is split into a frozen legacy control and a scaffold for a future performance-focused rewrite.

## Repository layout

- `src/main/java/legacy/` contains the complete pre-rewrite game engine, strategies, and simulation runner.
- `src/main/java/optimized/` contains the Phase 1 fixed-size bitboard engine and simple strategy ports.
- `src/test/java/` contains the existing legacy unit tests.
- `benchmarks/` is reserved for future cross-engine benchmarks.

## Legacy control

The legacy tree contains the original Battleship implementation and strategy logic. It is the behavioral and performance control for future work and should not normally receive performance improvements. Its algorithms, randomization, iteration order, tie-breaking, object allocation, exceptions, ship placement, hit/sink handling, and termination behavior are intentionally preserved.

If a genuine legacy bug is discovered after this freeze, document it before making any correction so benchmark results remain interpretable.

Run the legacy tests and simulation with:

```shell
./gradlew test
./gradlew run
```

The Gradle application entry point is `legacy.runner.Main`.

## Optimized implementation

Phase 1 provides a fixed 10×10 engine using two-long bitboards, primitive state transitions, precomputed placements, reusable state, and optimized Random, Parity, and RandomHunt strategy ports. Phase 2 adds exact recomputation and incremental CSR versions of `BoostedNoHuntProb`. Phase 3 incrementally maintains the frozen strategy's dynamic bonus and adjacency state through an immutable motif graph. Parallel simulation and SIMD batching remain intentionally unimplemented.

Run the Phase 1 single-thread comparison with:

```shell
./gradlew phase1Benchmark
./gradlew phase2Parity
./gradlew phase2Benchmark
./gradlew phase3Parity
./gradlew phase3Benchmark
```

## Benchmarks

Future benchmarks will compare equivalent strategies on the legacy and optimized engines. This keeps engine speedups separate from strategy changes. Planned comparisons include Random, Random + Hunt, Parity, Probability, Boosted Probability, and Boosted No-Hunt Probability variants.
