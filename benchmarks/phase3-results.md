# Phase 3 incremental bonus results

Measured on 2026-09-20 on arm64 macOS 26.6.2 with OpenJDK 23.0.1. All acceptance measurements were single-threaded and used identical deterministic fleet corpora. Reported A/B/C/D values are median elapsed-time trials after 2,000 warm-up games and three measured trials.

## Exact parity

Frozen legacy, Phase 2 incremental placement, and Phase 3 incremental bonus completed 100,000 common-oracle fleets with 4,472,870 turns compared and zero shot mismatches.

## Motif graph

The graph contains 360 directed adjacency motifs, 320 oriented `+15` motifs, and 240 oriented `+30` motifs: 920 total motifs and 1,720 HIT-to-motif incidences. Active motif state occupies 15 longs.

## A/B/C/D results

| Scale | Implementation | Games | Time (ms) | Games/sec | Total turns | Mean | Median | Min | Max |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10k | A: legacy | 10,000 | 674.106 | 14,834.5 | 445,951 | 44.59510 | 44 | 19 | 71 |
| 10k | B: optimized recompute | 10,000 | 566.690 | 17,646.3 | 445,951 | 44.59510 | 44 | 19 | 71 |
| 10k | C: Phase 2 incremental | 10,000 | 188.910 | 52,935.1 | 445,951 | 44.59510 | 44 | 19 | 71 |
| 10k | D: Phase 3 incremental bonus | 10,000 | 150.822 | 66,303.2 | 445,951 | 44.59510 | 44 | 19 | 71 |
| 100k | A: legacy | 100,000 | 6,658.316 | 15,018.8 | 4,476,315 | 44.76315 | 44 | 18 | 71 |
| 100k | B: optimized recompute | 100,000 | 5,717.514 | 17,490.1 | 4,476,315 | 44.76315 | 44 | 18 | 71 |
| 100k | C: Phase 2 incremental | 100,000 | 1,838.606 | 54,389.0 | 4,476,315 | 44.76315 | 44 | 18 | 71 |
| 100k | D: Phase 3 incremental bonus | 100,000 | 1,451.108 | 68,912.8 | 4,476,315 | 44.76315 | 44 | 18 | 71 |
| 1M | C: Phase 2 incremental | 1,000,000 | 18,439.937 | 54,230.1 | 44,691,132 | 44.69113 | 44 | 18 | 73 |
| 1M | D: Phase 3 incremental bonus | 1,000,000 | 14,688.287 | 68,081.5 | 44,691,132 | 44.69113 | 44 | 18 | 73 |

At 100,000 games, A→B is 1.1645×, B→C is 3.1097×, C→D is 1.2670×, and A→D is 4.5884×. At one million games, C→D remains 1.2554×.

## Dynamic operation characterization

Each scenario used 200,000 operations. Update scenarios include reset and any prerequisite hits so every iteration starts from valid reusable state.

| Scenario | Total (ms) | ns/operation |
|---|---:|---:|
| Bonus/strategy reset | 9.295 | 46.474 |
| Reset + MISS full update | 34.797 | 173.983 |
| Reset + isolated HIT | 19.719 | 98.593 |
| Reset + two aligned HITs | 33.430 | 167.150 |
| Reset + three aligned HITs | 46.085 | 230.426 |
| Reset + length-2 SUNK sequence | 77.850 | 389.250 |
| Reset + length-3 SUNK sequence | 113.993 | 569.966 |
| Reset + length-5 SUNK sequence | 199.393 | 996.963 |
| Candidate selection | 16.589 | 82.945 |

A miss performs zero dynamic motif or adjacency updates; its measured full-update cost is Phase 2 placement invalidation. These are controlled in-process loops, not JMH confidence intervals.

## Phase 3-only profile

A Java Flight Recorder run focused on 1.5 million D games found:

| Site | Samples |
|---|---:|
| 100-cell `chooseShot` scan | 41.02% |
| Benchmark runner/game loop | 20.84% |
| Placement invalidation | 18.61% |
| Motif satisfaction reevaluation | 6.83% |
| Placement cell traversal | 3.79% |
| HIT-to-motif collection | 2.48% |
| Dirty-motif iteration | 2.33% |
| Remaining track/update bookkeeping | 2.23% |
| Optimized engine shot | 0.56% |

No Phase 3 per-turn allocation site was observed. Allocation samples came from corpus construction and once-per-batch result arrays.

The evidence-based Phase 4 target is candidate selection/argmax maintenance. Motif work is no longer dominant, and physical placement deletion remains unjustified.
