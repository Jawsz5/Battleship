# Phase 4 incremental candidate-selection results

Measured on 2026-09-20 on arm64 macOS 26.6.2 with OpenJDK 23.0.1. Measurements are single-threaded, use identical deterministic fleet corpora, and report the median elapsed-time trial after warm-up.

## Design and exact ordering

The selected implementation is `BucketSelectionBoostedNoHuntStrategy`. It keeps a 510-entry (`255 scores × 2 adjacency states`) array of two-long 100-cell buckets. A key is `(totalScore << 1) | adjacent`; consequently, the greatest key applies the required score-descending and adjacent-first order. `Long.numberOfTrailingZeros` on the low word, then high word, supplies the required smallest cell ID.

The literal frozen scan is independently retained in `AbstractIncrementalSelectionStrategy.referenceBest()`. Invariants check the indexed score, adjacency, and selectable state for all 100 cells, membership of every bucket/tree entry, and equality with that literal scan at every stable test transition.

The score range is bounded by the immutable tables:

| Bound | Value | Derivation |
|---|---:|---|
| `MIN_TOTAL_SCORE` | 0 | Scores only contain active-placement and satisfied-motif contributions. |
| `MAX_BASE_SCORE` | 34 | Maximum initial `PlacementCsr.INITIAL_BASE_SCORE` entry. |
| `MAX_BONUS_SCORE` | 220 | Maximum per-target sum of all immutable motif weights. |
| `MAX_TOTAL_SCORE` | 254 | Safe exact upper bound: 34 + 220. |
| Score values | 255 | Inclusive range 0–254. |

`AbstractIncrementalSelectionStrategy` batches score, adjacency, and fired-state changes in `dirtyCellLo/dirtyCellHi`. A transition reconciles each dirty cell at most once using its stored `indexedScore`, `indexedAdjacent`, and `indexedSelectable` values. No board-wide ranking rebuild occurs after a shot.

## Candidate designs

| Design | Lookup | Update cost | Full-game result |
|---|---|---|---|
| D: Phase 3 linear scan | 100-cell scan | Existing Phase 3 updates | Control |
| E1: packed score buckets | Highest occupied key + trailing-zero extraction | One remove/insert reconciliation per dirty cell | **Winner** |
| E2: 128-leaf tournament tree | Root winner | Seven-level propagation per dirty cell | Rejected |

The tournament lookup is fastest in isolation, but its propagation cost dominates full games. A cached-best implementation was not retained because the currently selected candidate is fired every turn, which forces a repair scan on nearly every observation and offers no credible full-game advantage over D/E1.

## Exact parity and invariants

`Phase4ParityCampaign` ran frozen legacy, Phase 1 recompute, Phase 2 incremental placement, Phase 3 incremental bonus, E1 buckets, and E2 tournament against a common optimized-game oracle.

| Fleets | Turns compared | Shot mismatches |
|---:|---:|---:|
| 100,000 | 4,468,373 | 0 |

The suite contains exhaustive two-candidate ranking-order verification across the complete score domain, score-domain verification, bucket/tree structural invariants, targeted miss/hit/sink and 63/64-boundary transitions, 500 selected-shot games, 500 randomized valid-observation games, and a 10,000-fleet JUnit campaign. `./gradlew test` completed 54 tests successfully.

## Raw full-game benchmark

The initial three-design run used 10,000 warm-up games, three trials, 100,000-game 100k scale, and 1,000,000-game 1m scale.

| Scale | Implementation | Games | Time (ms) | Games/sec | Total turns | Mean | Median | Min | Max |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10k | D: linear scan | 10,000 | 145.778 | 68,597.3 | 447,705 | 44.77050 | 44 | 20 | 71 |
| 10k | E1: score buckets | 10,000 | 146.449 | 68,283.0 | 447,705 | 44.77050 | 44 | 20 | 71 |
| 10k | E2: tournament | 10,000 | 316.537 | 31,591.9 | 447,705 | 44.77050 | 44 | 20 | 71 |
| 100k | D: linear scan | 100,000 | 1,468.513 | 68,096.1 | 4,466,951 | 44.66951 | 44 | 19 | 73 |
| 100k | E1: score buckets | 100,000 | 1,400.773 | 71,389.2 | 4,466,951 | 44.66951 | 44 | 19 | 73 |
| 100k | E2: tournament | 100,000 | 3,066.394 | 32,611.6 | 4,466,951 | 44.66951 | 44 | 19 | 73 |
| 1m | D: linear scan | 1,000,000 | 14,869.588 | 67,251.4 | 44,688,553 | 44.68855 | 44 | 18 | 73 |
| 1m | E1: score buckets | 1,000,000 | 14,201.720 | 70,414.0 | 44,688,553 | 44.68855 | 44 | 18 | 73 |
| 1m | E2: tournament | 1,000,000 | 30,584.638 | 32,696.2 | 44,688,553 | 44.68855 | 44 | 18 | 73 |

After inlining the E1 old-key removal and new-key insertion into a single reconciliation primitive, a final paired, three-trial 1m measurement on a fresh deterministic corpus produced:

| Implementation | Games/sec | Total turns |
|---|---:|---:|
| D: linear scan | 67,477.9 | 44,687,873 |
| E1: score buckets | 74,077.1 | 44,687,873 |

Final D→E1 speedup: **1.0978×** (+9.78%).

## Update versus lookup cost

The following raw supporting loops ran 200,000 valid reusable-state operations. Update rows include reset and prerequisite observations; they are not JMH confidence intervals.

| Architecture/scenario | ns/operation |
|---|---:|
| Scan reset | 52.372 |
| Scan central-miss update | 200.052 |
| Scan isolated-hit update | 110.093 |
| Scan three-hit update | 226.449 |
| Scan length-5 sink update | 928.863 |
| Scan candidate lookup | 74.197 |
| Bucket reset | 201.197 |
| Bucket central-miss update | 502.440 |
| Bucket isolated-hit update | 312.257 |
| Bucket three-hit update | 560.340 |
| Bucket length-5 sink update | 2,169.725 |
| Bucket candidate lookup | 10.715 |
| Tournament reset | 209.962 |
| Tournament central-miss update | 910.114 |
| Tournament isolated-hit update | 485.356 |
| Tournament three-hit update | 1,142.235 |
| Tournament length-5 sink update | 3,857.086 |
| Tournament candidate lookup | 5.087 |

So E1 does move some time into update bookkeeping, but the full-game data shows its much cheaper selection still wins overall. E2's even cheaper lookup cannot amortize tree maintenance.

## Focused profile and allocation evidence

Focused 500,000-game JFR recordings are retained as `phase4-scan.jfr` and `phase4-bucket.jfr` in this directory. The final E1 recording measured 77,570.5 games/sec on its focused corpus. Its largest sampled methods were bucket reconciliation (46.28%), placement invalidation (17.38%), motif reevaluation (9.04%), invalidation-through-mask (6.91%), motif incident collection (6.21%), and bucket lookup (0.53%). Candidate selection has therefore largely disappeared as a lookup hotspot; ranking maintenance is the largest remaining optimization target.

JFR observed no `ObjectAllocationInNewTLAB` or `ObjectAllocationOutsideTLAB` events and no GC events during the focused runs. Allocation samples were setup/batch work (`generateCorpus`, class initialization, and result sorting), not per-turn strategy work. The hot path uses primitive arrays and long bitboards only.

## Recommendation

Retain E1 as the Phase 4 selector. Exact semantics and single-thread throughput both improve materially, no legacy/control semantics changed, and no per-turn allocation was observed. The game state and winner selector are per-instance with no shared mutable state, so the implementation is ready for an independent-game multicore Phase 5; this phase intentionally implements neither multithreading nor SIMD.
