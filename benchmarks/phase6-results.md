# Phase 6 SIMD / batched execution and final engine freeze

Measured on 2026-09-20 on an Apple M4 Max (10 performance + 4 efficiency physical cores, 14 logical processors), macOS 26.6.2, with OpenJDK 23.0.1. Phase 6 is the final engine-performance phase; future work is strategy research.

## Experiments

The local JDK exposes `jdk.incubator.vector@23.0.1`; Gradle uses `--add-modules jdk.incubator.vector`, and the preferred `IntVector` species has width 4. A warmed dense 100-element integer-add proof-of-concept measured up to 2.86× scalar arithmetic, but this sparse/local operation is not representative of Phase 4 updates. No accepted game or strategy state uses Vector API instructions.

`BatchedSimulationRunner` was implemented with complete Phase 4 game and strategy state per lane. It processes one turn per active lane and immediately refills completed lanes. Widths 1, 2, 4, 8, and 16 were tested. This remains a scalar lane experiment; no forced SoA rewrite was made because strategy updates are sparse and divergent. Structure-of-arrays and true bit-sliced strategy state were not implemented.

## Exact correctness

For 10,000 indexed deterministic fleets, the scalar runner and width-8 batch runner produced identical selected-shot sequences: **447,169 turns compared and zero shot mismatches**. Widths 1, 2, 4, 8, and 16 also produced identical games, total turns, histograms, means, medians, minima, and maxima. The full suite contains 61 passing tests.

## Batch-width results

One-million-game deterministic corpus, three median trials:

| Width | Single-worker games/sec | 14-worker games/sec |
|---:|---:|---:|
| 1 | 82,610.0 | 859,337.6 |
| 2 | 76,025.0 | 847,100.6 |
| 4 | 77,155.7 | 853,440.1 |
| 8 | 78,310.6 | 864,090.4 |
| 16 | 76,209.3 | 772,989.3 |

The required stabilized 10M comparison selected the scalar implementation:

| Configuration | Games/sec | Total turns |
|---|---:|---:|
| 14-worker scalar Phase 5 control | 824,864.5 | 446,931,211 |
| 14-worker width-8 batch | 754,445.3 | 446,931,211 |

Batching was rejected as canonical: −8.53% on the final workload. The short 1M width-8 result did not survive the longer run.

## Final engine

| Field | Value |
|---|---|
| `FINAL_ENGINE_VARIANT` | Phase 5 `ParallelSimulationRunner` with Phase 4 bucket strategy |
| `FINAL_WORKER_COUNT` | 14 on this M4 Max |
| `FINAL_BATCH_WIDTH` | 1 (scalar independent games) |
| `FINAL_SIMD_MODE` | disabled; Vector API proof-of-concept retained only |
| Peak throughput | 845,535.4 games/sec |
| Peak turn throughput | 37,787,247.6 turns/sec |

The accepted Phase 5 peak remains canonical: 845,535.4 games/sec, 11.0114× its one-worker run, and 56.2985× the accepted legacy baseline of 15,018.8 games/sec. Phase 6 therefore has a Phase 5 speedup of 1.0000×.

## Allocation, profile, and freeze

The scalar Phase 5 steady-state JFR profile remains authoritative: no worker-loop allocations, no steady-state GC, and no contention events. Batch lane state is allocated once and has no per-turn objects. Profiles remain dominated by bucket reconciliation, dirty-cell reconciliation, motif work, and placement invalidation; lane management and vector arithmetic did not remove those sparse divergent costs.

The scalar Phase 5 runner plus `BucketSelectionBoostedNoHuntStrategy` is the frozen canonical research platform. Engine optimization is complete. Future work is strategy research, not another engine-performance phase.
