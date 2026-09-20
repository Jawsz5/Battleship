# Phase 1 benchmark results

Measured on 2026-09-20 on arm64 macOS 26.6.2 with OpenJDK 23.0.1. The harness ran single-threaded with 2,000 warm-up games followed by five measured trials of 10,000 games per implementation and strategy. The table reports the median elapsed-time trial.

| Implementation | Strategy | Games | Time (ms) | Games/sec | Mean turns | Median | Min | Max |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Legacy | Random | 10,000 | 65.871 | 151,812.3 | 95.406 | 97 | 67 | 100 |
| Optimized | Random | 10,000 | 10.432 | 958,608.1 | 95.397 | 97 | 67 | 100 |
| Legacy | Parity | 10,000 | 212.029 | 47,163.4 | 56.905 | 57 | 28 | 100 |
| Optimized | Parity | 10,000 | 77.938 | 128,306.5 | 57.091 | 57 | 25 | 100 |
| Legacy | RandomHunt | 10,000 | 319.285 | 31,320.0 | 66.321 | 65 | 29 | 100 |
| Optimized | RandomHunt | 10,000 | 92.985 | 107,543.7 | 66.176 | 65 | 27 | 100 |

The stochastic distributions are consistent in mean and median; exact outcomes are not expected because the optimized runner uses an injected `SplittableRandom` while legacy code uses global random sources.

## Optimized core-operation characterization

| Operation | Iterations | Total (ms) | ns/operation |
|---|---:|---:|---:|
| Legacy fleet generation | 100,000 | 131.163 | 1,311.630 |
| Optimized fleet generation | 1,000,000 | 37.498 | 37.498 |
| Fixed-state reset | 1,000,000 | 19.431 | 19.431 |
| Game-over check | 1,000,000 | 2.688 | 2.688 |
| Ship-sunk check | 1,000,000 | 2.936 | 2.936 |
| Overlap check | 1,000,000 | 2.567 | 2.567 |
| Shot resolution | 999,991 | 6.167 | 6.167 |

These are simple in-process loop measurements, not JMH confidence intervals, and should be treated as characterization rather than universal hardware-independent latency claims.

## Flight Recorder findings

A second, larger run was sampled with Java Flight Recorder. The dominant legacy sites were `ArrayList.remove(Object)`, legacy RandomHunt selection, repeated `Ship.isSunk()` scans, and array growth/copying. The principal optimized sites were ordered hunt-frontier traversal, Parity candidate construction, nth-set-bit selection, and RandomHunt candidate construction. `OptimizedGame.shoot` represented 1.85% of execution samples and optimized fleet generation represented 0.93%.

Allocation sampling found no optimized per-turn allocation site. The only optimized allocation reported was `SimulationRunner.run`, which creates one turn-count array per measured batch. Legacy array copying, exception stack traces during placement, lists, iterators, ships, and strategy construction dominated allocation pressure.
