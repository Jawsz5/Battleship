# Phase 2 incremental probability results

Measured on 2026-09-20 on arm64 macOS 26.6.2 with OpenJDK 23.0.1. All probability comparisons were single-threaded and used identical deterministic fleet layouts. Each reported A/B/C result is the median elapsed-time trial after 2,000 warm-up games and three measured trials.

## Exact parity

The frozen `BoostedNoHuntProb`, optimized recomputation control, and incremental CSR implementation completed 100,000 common-oracle fleets with 4,472,796 shot decisions compared and zero mismatches.

## A/B/C results

| Scale | Implementation | Games | Time (ms) | Games/sec | Total turns | Mean | Median | Min | Max |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10k | A: legacy | 10,000 | 685.900 | 14,579.4 | 445,951 | 44.59510 | 44 | 19 | 71 |
| 10k | B: optimized + recompute | 10,000 | 668.280 | 14,963.8 | 445,951 | 44.59510 | 44 | 19 | 71 |
| 10k | C: optimized + incremental | 10,000 | 193.654 | 51,638.6 | 445,951 | 44.59510 | 44 | 19 | 71 |
| 100k | A: legacy | 100,000 | 6,777.436 | 14,754.8 | 4,476,315 | 44.76315 | 44 | 18 | 71 |
| 100k | B: optimized + recompute | 100,000 | 6,640.050 | 15,060.1 | 4,476,315 | 44.76315 | 44 | 18 | 71 |
| 100k | C: optimized + incremental | 100,000 | 1,920.996 | 52,056.3 | 4,476,315 | 44.76315 | 44 | 18 | 71 |
| 1M | C: optimized + incremental | 1,000,000 | 19,110.563 | 52,327.1 | 44,691,132 | 44.69113 | 44 | 18 | 73 |

At the 100,000-game scale, A→B is 1.0207×, B→C is 3.4566×, and A→C is 3.5281×. The identical turn totals establish that these are architectural speedups, not strategy changes.

## Probability operation characterization

These controlled loops used 200,000 operations. Update scenarios include reset so each operation begins from valid reusable state.

| Scenario | Total (ms) | ns/operation |
|---|---:|---:|
| Incremental reset | 9.012 | 45.062 |
| Reset + ordinary HIT update | 10.682 | 53.410 |
| Reset + central MISS invalidation | 38.180 | 190.899 |
| Reset + length-3 SUNK update | 73.771 | 368.856 |
| Full recompute selection | 383.027 | 1,915.135 |
| Incremental selection | 38.876 | 194.379 |

The measurements are in-process characterization loops rather than JMH confidence intervals.

## Flight Recorder findings

In a profiled mixed A/B/C workload, frozen recomputation accounted for 25.76% of samples and optimized-control recomputation for 23.04%. The largest incremental site was dynamic bonus recomputation at 25.44% of all samples. Placement invalidation accounted for 7.20%, cell-to-placement traversal for 1.60%, and sink handling for 0.16%.

No incremental per-turn allocation site appeared in allocation sampling. Reported optimized allocations came from pre-generating benchmark corpora; legacy ships, oceans, strategy construction, arrays, lists, and iterators dominated measured allocation pressure.

The strongest next single-thread target is dynamic bonus calculation. Static CSR entries should not be physically deleted yet: inactive-entry traversal was small, and the measured bonus work is substantially larger.
