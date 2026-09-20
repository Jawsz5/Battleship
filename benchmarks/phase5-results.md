# Phase 5 multicore independent-game simulation results

Measured on 2026-09-20 on a MacBook Pro (Mac16,6) with an Apple M4 Max (10 performance + 4 efficiency physical cores; 14 logical processors exposed to the JVM), 36 GB memory, macOS 26.6.2, and OpenJDK 23.0.1. All principal measurements use a reusable fixed platform-thread pool, three trials, and median elapsed time. Thread creation is outside each timed simulation batch.

## Parallel architecture

`ParallelSimulationRunner` creates exactly one long-running task per worker. Every `SimulationWorker` owns an `OptimizedGame`, `BucketSelectionBoostedNoHuntStrategy`, `SplittableRandom`, two five-long fleet scratch arrays, and a fixed 101-bin turn histogram. There are no atomics, locks, shared counters, or synchronization in the game/turn loop. `Future.get()` and reduction occur only after each worker completes its coarse static partition.

The random mode derives worker `i`'s seed as `mix64(masterSeed + GOLDEN_GAMMA * i)`. Thus the same master seed, game count, and worker count reproduce the same aggregate result. For worker-count-independent correctness, corpus mode generates deterministic fleets once, partitions contiguous game ranges with counts differing by at most one, and treats the corpus arrays as read-only.

The histogram replaces batch-sized per-game turn arrays. It produces exact global game count, turn sum, min, max, mean, and median without storing a result per game.

### Static-table audit

| State | Classification | Worker behavior |
|---|---|---|
| `BitBoard100`, `ShipPlacements` geometry | Immutable after class initialization | Shared read-only |
| `PlacementCsr`, `BonusMotifGraph` tables, initial score tables | Mutable Java arrays by type but never written by workers | Shared read-only by invariant |
| Corpus `low/high` arrays | Input-only during a run | Shared read-only; copied into worker scratch arrays |
| Game masks, strategy arrays/bitsets/buckets/dirty masks, RNG, scratch arrays, histogram/counters | Mutable | One instance per worker |

## Correctness

The same 10,000-fleet corpus produced exactly equal games, total turns, histogram, mean, median, minimum, and maximum under 1, 2, and 4 workers. The test suite also compares the histogram result to the pre-existing single-thread runner, repeats a 2,000-fleet 4-worker corpus run ten times, and repeats a seeded 20,000-game random run for reproducibility.

The 1M corpus scaling table below likewise has the identical total-turn count (44,698,640), mean (44.69864), median (44), min (18), and max (73) at every worker count.

## Scaling: exact deterministic corpus

| Workers | Games | Time (ms) | Games/sec | Speedup | Efficiency |
|---:|---:|---:|---:|---:|---:|
| 1 | 1,000,000 | 13,010.383 | 76,861.7 | 1.0000 | 1.0000 |
| 2 | 1,000,000 | 6,448.281 | 155,080.1 | 2.0177 | 1.0088 |
| 4 | 1,000,000 | 3,207.357 | 311,783.2 | 4.0564 | 1.0141 |
| 8 | 1,000,000 | 1,600.301 | 624,882.6 | 8.1300 | 1.0162 |
| 10 | 1,000,000 | 1,434.017 | 697,342.0 | 9.0727 | 0.9073 |
| 14 | 1,000,000 | 1,246.717 | 802,106.5 | 10.4357 | 0.7454 |

## Scaling: worker-local fleet generation

This mode is the throughput-oriented simulation workload; it includes fleet generation in every worker and therefore is intentionally distinct from the fixed-corpus control above.

| Workers | Games | Time (ms) | Games/sec | Speedup | Efficiency |
|---:|---:|---:|---:|---:|---:|
| 1 | 1,000,000 | 13,029.310 | 76,750.0 | 1.0000 | 1.0000 |
| 2 | 1,000,000 | 6,456.174 | 154,890.5 | 2.0181 | 1.0091 |
| 4 | 1,000,000 | 3,221.913 | 310,374.6 | 4.0440 | 1.0110 |
| 8 | 1,000,000 | 1,618.353 | 617,912.0 | 8.0510 | 1.0064 |
| 10 | 1,000,000 | 1,444.766 | 692,153.6 | 9.0183 | 0.9018 |
| 14 | 1,000,000 | 1,291.486 | 774,302.2 | 10.0886 | 0.7206 |

The matching five-million-game saturation run confirms the peak rather than relying on the shorter sample:

| Workers | Games | Time (ms) | Games/sec | Speedup | Efficiency |
|---:|---:|---:|---:|---:|---:|
| 1 | 5,000,000 | 65,115.213 | 76,787.0 | 1.0000 | 1.0000 |
| 8 | 5,000,000 | 8,513.396 | 587,309.7 | 7.6487 | 0.9561 |
| 10 | 5,000,000 | 7,852.930 | 636,705.1 | 8.2920 | 0.8292 |
| 14 | 5,000,000 | 5,913.413 | 845,535.4 | 11.0114 | 0.7865 |

The throughput gain continues past the ten performance cores, but efficiency declines as the four efficiency cores join the integer-heavy workload. Fourteen is nevertheless the measured peak; no SMT behavior is involved because this M4 Max exposes physical cores directly.

## Peak throughput and historical comparison

The best result is **14 workers**, **845,535.4 games/sec**, or approximately **37,787,247.6 turns/sec** (`845,535.4 × 44.69032`).

| Comparison | Throughput | Multiplier |
|---|---:|---:|
| Phase 3 accepted legacy `BoostedNoHuntProb`, single thread | 15,018.8 games/sec | baseline |
| Phase 4 accepted bucket selector, single thread | 74,077.1 games/sec | 4.9323× legacy |
| Phase 5 worker-local-random, 14 workers | 845,535.4 games/sec | 11.4143× Phase 4; 56.2985× legacy |

The Phase 4 and Phase 5 one-worker measurements are both about 76.8k games/sec; Phase 5's historic comparison is therefore not conflating a changed single-game strategy with parallel scaling. Corpus and random modes are separately reported because fixed-corpus execution excludes fleet generation while random mode includes it.

## Allocation, GC, and profile

The delayed 14-worker JFR recording (`phase5-14worker-steady.jfr`) started after warmup and recorded eight seconds of a 10M-game run. It found no worker-loop allocation samples and no GC events. Allocation samples were JIT/classfile infrastructure and JFR periodic machinery. A startup-inclusive recording did show runtime/class-loading allocation and one 1.36 ms young GC, so that cold-start activity is deliberately excluded from steady-state claims.

No contention events were recorded. The startup-inclusive profile showed all fourteen `battleship-sim-*` workers with comparable user CPU shares (about 6% each). The steady-state hottest methods were bucket reconciliation (29.30%), dirty-cell reconciliation (21.21%), HIT-to-motif collection (12.64%), placement invalidation (9.49%), motif reevaluation (6.97%), and invalidation-through-mask (5.76%). Bucket lookup was only 0.37%; fleet generation was 0.24%. Scheduling and final statistics reduction were not material hotspots.

## Recommendation for Phase 6

Phase 5 meets its multicore goal. Further independent-thread scaling is saturated at the 14 physical cores. A Phase 6 SIMD/bit-sliced experiment is justified only as a separate algorithmic study, with the best candidate being per-game dirty-cell bucket reconciliation and placement/motif update work—not task scheduling, result aggregation, or candidate lookup. No SIMD or batched-game implementation was added here.
