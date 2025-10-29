package compstrategies.probstrats.HitMaxOptimize;

import java.util.Arrays;

import compstrategies.probstrats.ShipMaxProb;

public class BoostedNoHuntProb extends ShipMaxProb {

    public BoostedNoHuntProb(int dimension){
        super(dimension);
    }

    // Hunt-free selectShot: recompute -> pick argmax(prob) among unknown cells.
    // Do NOT pre-mark the guess in hitMap; let trackShot() update after result.
    @Override
    public int selectShot() {
        recompute();

        int bestId = -1, bestScore = Integer.MIN_VALUE;

        // tiny tiebreaker: prefer cells adjacent to hits (keeps runs tight)
        for (int id = 0; id < nCells; id++) {
            if (hitMap[id] != 0) continue; // only unknown cells
            int s = prob[id];
            if (s > bestScore) {
                bestScore = s;
                bestId = id;
            } else if (s == bestScore && bestId != -1) {
                // tiebreak: prefer adjacency to a hit
                if (adjacentToHit(id) && !adjacentToHit(bestId)) bestId = id;
            }
        }
        if (bestId < 0) throw new IllegalStateException("No cells left to fire at");
        return bestId;
    }

    private boolean adjacentToHit(int id){
        int r = id / dim, c = id % dim;
        return (r > 0         && hitMap[(r-1)*dim + c] == 2) ||
               (r+1 < dim     && hitMap[(r+1)*dim + c] == 2) ||
               (c > 0         && hitMap[r*dim + (c-1)] == 2) ||
               (c+1 < dim     && hitMap[r*dim + (c+1)] == 2);
    }

    // Hunt-free tracking: no Hunt object, same sunk ship tracking.
    @Override
    public void trackShot(boolean hit, int sunkLen, int pos, int[] sunkCells) {
        hitMap[pos] = hit ? (byte)2 : (byte)1;

        // mark sunk ship cells & decrement remaining count
        if (hit && sunkLen > 1 && sunkLen < 6 && remain[sunkLen] > 0 && sunkCells != null) {
            remain[sunkLen]--;
            int limit = Math.min(sunkLen, sunkCells.length);
            for (int k = 0; k < limit; k++) {
                int cid = sunkCells[k];
                if (cid >= 0 && cid < nCells) hitMap[cid] = 3;
            }
        }
    }

    // Probabilities: base placements + adjacency/line bonuses.
    @Override
    protected void recompute() {
        Arrays.fill(prob, 0);

        for (int L = 2; L <= 5; L++) {
            int copies = remain[L];
            if (copies == 0) continue;

            // horizontal placements
            for (int r = 0; r < dim; r++) {
                for (int c = 0; c <= dim - L; c++) {
                    int base = r * dim + c;
                    boolean blocked = false;
                    for (int k = 0; k < L; k++) {
                        int id = base + k;
                        byte h = hitMap[id];
                        if (h == 1 || h == 3) { blocked = true; break; } // miss/sunk blocks
                    }
                    if (blocked) continue;
                    for (int k = 0; k < L; k++) {
                        int id = base + k;
                        if (hitMap[id] == 0) prob[id] += copies;
                    }
                }
            }

            // vertical placements
            for (int c = 0; c < dim; c++) {
                for (int r = 0; r <= dim - L; r++) {
                    int base = r * dim + c;
                    boolean blocked = false;
                    for (int k = 0; k < L; k++) {
                        int id = base + k * dim;
                        byte h = hitMap[id];
                        if (h == 1 || h == 3) { blocked = true; break; }
                    }
                    if (blocked) continue;
                    for (int k = 0; k < L; k++) {
                        int id = base + k * dim;
                        if (hitMap[id] == 0) prob[id] += copies;
                    }
                }
            }
        }

        // adjacency bonuses around unsunk hits
        // for larger board sizes, award larger bonuses
        final int ADJ = 10;   // neighbor of a hit
        final int LINE = 15;  // extends a line of hits

        for (int id = 0; id < nCells; id++) {
            if (hitMap[id] != 2) continue; // only around hits
            int r = id / dim, c = id % dim;

            boost(r + 1, c, ADJ);
            boost(r - 1, c, ADJ);
            boost(r, c + 1, ADJ);
            boost(r, c - 1, ADJ);

            // line-extension
            
            if (in(r - 1, c) && hitMap[(r - 1) * dim + c] == 2) {
                boost(r + 1, c, LINE);
                if (in(r - 2, c) && hitMap[(r - 2) * dim + c] == 2) boost(r + 2, c, LINE*2);
            }
            if (in(r + 1, c) && hitMap[(r + 1) * dim + c] == 2){
                boost(r - 1, c, LINE);
                if (in(r + 2, c) && hitMap[(r + 2) * dim + c] == 2) boost(r - 2, c, LINE*2);
            } 
            if (in(r, c - 1) && hitMap[r * dim + (c - 1)] == 2){
                boost(r, c + 1, LINE);
                if (in(r, c - 2) && hitMap[r * dim + (c - 2)] == 2) boost(r, c + 2, LINE*2);
                
            }
            if (in(r, c + 1) && hitMap[r * dim + (c + 1)] == 2){
                boost(r, c - 1, LINE);
                if (in(r, c + 2) && hitMap[r * dim + (c + 2)] == 2) boost(r, c - 2, LINE*2);
            }
        }
    }

    private boolean in(int r, int c) { return r >= 0 && r < dim && c >= 0 && c < dim; }

    private void boost(int r, int c, int val) {
        if (!in(r, c)) return;
        int id = r * dim + c;
        if (hitMap[id] == 0) prob[id] += val; // never add to miss/hit/sunk cells
    }
}
