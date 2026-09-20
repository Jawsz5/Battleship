# Optimized implementation

Phase 1 implements a fixed 10x10 Battleship engine. Cells use row-major IDs (`row * 10 + col`): cells 0-63 occupy `lo`, and cells 64-99 occupy the low 36 bits of `hi`.

The engine keeps shots, remaining occupied cells, and five immutable ship masks in primitive fields/arrays. A shot is a constant-time bit transition; only five mask checks are needed to identify a hit ship. Legal placements for lengths 2, 3, 4, and 5 are precomputed once into parallel `long[]` tables. Random fleets select from those tables and reject overlaps without exceptions.

`OptimizedGame`, each strategy, and their primitive scratch arrays are reused across games. `ShotResult` encodes miss/hit/sunk and sunk length in one `int`; the engine exposes a sunk mask only after a sink, so strategies cannot inspect the hidden fleet.

Phase 1 ports Random, Parity, and RandomHunt. Phase 2 adds a literal full-recomputation `BoostedNoHunt` control and an exact incremental implementation backed by static placement-to-cell and cell-to-placement CSR arrays, a 10-long active-placement bitset, incremental base scores, and per-length coverage counts.

The Phase 2 graph contains 600 legal placements and 2,000 placement/cell incidences. Misses and sunk cells invalidate placements monotonically; ordinary hits only affect dynamic bonuses. Phase 5 supplies the frozen scalar multicore runner; Phase 6 evaluated Vector API and lane-refilling batching experimentally, but the scalar runner remained faster on the stabilized workload.

Phase 3 preserves that placement architecture and adds 920 immutable bonus motifs with 1,720 reverse HIT-to-motif incidences. Motif activation maintains `bonusScore[100]`, while `adjacentHitCount[100]` preserves the legacy tie rule without repeated neighbor checks. Misses cause no motif work; hits and sinks reevaluate only motifs incident to changed unsunk-hit cells.
