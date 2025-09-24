package compstrategies.probstrats;

import java.util.*;

public class TrickyProbMap extends recalculateProbMap {

    public TrickyProbMap(int dimension){
        super(dimension);
    }

    @Override
    protected void recompute() {
        Arrays.fill(prob, 0);

        // --- 1) Base placement counts (no coversHit gating) ---
        for (int L = 2; L <= 5; L++) {
            int copies = remain[L];
            if (copies == 0) continue;

            // Horizontal placements
            for (int r = 0; r < dim; r++) {
                for (int c = 0; c <= dim - L; c++) {
                    int base = r * dim + c;
                    boolean blocked = false;
                    for (int k = 0; k < L; k++) {
                        int id = base + k;
                        byte h = hitMap[id];
                        if (h != 0) { // miss or sunk blocks
                            blocked = true; break;
                        }
                    }
                    if (blocked) continue;
                    for (int k = 0; k < L; k++) {
                        int id = base + k;
                        if (hitMap[id] == 0) prob[id] += copies;
                    }
                }
            }

            // Vertical placements
            for (int c = 0; c < dim; c++) {
                for (int r = 0; r <= dim - L; r++) {
                    int base = r * dim + c;
                    boolean blocked = false;
                    for (int k = 0; k < L; k++) {
                        int id = base + k * dim;
                        byte h = hitMap[id];
                        if (h != 0) { blocked = true; break; }
                    }
                    if (blocked) continue;
                    for (int k = 0; k < L; k++) {
                        int id = base + k * dim;
                        if (hitMap[id] == 0) prob[id] += copies;
                    }
                }
            }
        }

        // --- 2) Adjacency bonuses around hits (Python-style) ---
        final int ADJ = 10;   // +10 if neighbor of a hit
        final int LINE = 15;  // +15 if extending a line of hits

        for (int id = 0; id < nCells; id++) {
            if (hitMap[id] != 2) continue; // only around unsunk hits
            int r = id / dim, c = id % dim;

            // four neighbors
            boost(r + 1, c, ADJ);
            boost(r - 1, c, ADJ);
            boost(r, c + 1, ADJ);
            boost(r, c - 1, ADJ);

            // line-extension bonus: if the opposite neighbor is also a hit,
            // increase the other side by +15 (mirrors their +15 heuristic)
            if (in(r - 1, c) && hitMap[(r - 1) * dim + c] == 2) boost(r + 1, c, LINE);
            if (in(r + 1, c) && hitMap[(r + 1) * dim + c] == 2) boost(r - 1, c, LINE);
            if (in(r, c - 1) && hitMap[r * dim + (c - 1)] == 2) boost(r, c + 1, LINE);
            if (in(r, c + 1) && hitMap[r * dim + (c + 1)] == 2) boost(r, c - 1, LINE);
        }
    }

    private boolean in(int r, int c) { return r >= 0 && r < dim && c >= 0 && c < dim; }

    private void boost(int r, int c, int val) {
        if (!in(r, c)) return;
        int id = r * dim + c;
        if (hitMap[id] == 0) prob[id] += val;  // never add to already-hit/miss/sunk
    }
}