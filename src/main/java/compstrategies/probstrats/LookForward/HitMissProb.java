package compstrategies.probstrats.LookForward;

import java.util.Arrays;
import compstrategies.probstrats.ShipMaxProb;

public class HitMissProb extends ShipMaxProb {
    // Expected-reduction score per cell: score[i] = 2 * count[i] * (T - count[i])
    protected int[] cellProbScore;

    public HitMissProb(int dimension) {
        super(dimension);
        cellProbScore = new int[nCells];
    }

    @Override
    public int selectShot() {
        int shot;
        
        // Keep Hunt behavior as-is
        if (h != null) {
            try {
                while (true) {
                    shot = h.huntShip(hitMap);
                    if (shot >= 0 && shot < hitMap.length && hitMap[shot] == 0) {
                        hitMap[shot] = 1; // mark attempted in Hunt
                        return shot;
                    }
                }
            } catch (Exception e) {
                h = null; // fallback to probability
            }
        }
        
        // Probability mode
        genHitMissProb();
        int bestId = -1, bestScore = Integer.MIN_VALUE;
        for (int id = 0; id < nCells; id++) {
            if (hitMap[id] != 0) continue;
            int s = cellProbScore[id];
            if (s > bestScore) { bestScore = s; bestId = id; }
        }
        if (bestId < 0) throw new IllegalStateException("No cells left to fire at");
        return bestId;
    }

    // Build expected-reduction score without re-running full recompute per cell
    protected void genHitMissProb() {
        Arrays.fill(cellProbScore, 0);

        // 1) Count how many feasible placements cover each unknown cell; also get T
        int[] coverCount = new int[nCells];  // count[i]
        int T = buildCoverCounts(coverCount); // total feasible placements (with copies)

        if (T <= 0) return; // nothing left

        // 2) Score(i) = 2 * count[i] * (T - count[i])  (maximize reduction; no division)
        for (int i = 0; i < nCells; i++) {
            if (hitMap[i] != 0) continue;
            int c = coverCount[i];
            cellProbScore[i] = 2 * c * (T - c);
        }
    }

    // Mirrors your placement loops; returns total placements T and fills coverCount
    private int buildCoverCounts(int[] coverCount) {
        Arrays.fill(coverCount, 0);
        int total = 0;

        // Optional: “must cover an existing hit if any hits exist” (like your final map).
        boolean haveHit = false;
        for (byte b : hitMap) { if (b == 2) { haveHit = true; break; } }

        // Iterate ship lengths that remain
        for (int L = 2; L <= 5; L++) {
            int copies = remain[L];
            if (copies == 0) continue;

            // Horizontal placements
            for (int r = 0; r < dim; r++) {
                for (int c0 = 0; c0 <= dim - L; c0++) {
                    int base = r * dim + c0;
                    boolean blocked = false, coversHit = !haveHit;
                    for (int k = 0; k < L; k++) {
                        int id = base + k;
                        byte h = hitMap[id];
                        if (h == 1 || h == 3) { blocked = true; break; } // miss or sunk blocks
                        if (h == 2) coversHit = true;
                    }
                    if (blocked || !coversHit) continue;

                    total += copies;
                    for (int k = 0; k < L; k++) {
                        int id = base + k;
                        if (hitMap[id] == 0) coverCount[id] += copies; // only unknown cells matter
                    }
                }
            }

            // Vertical placements
            for (int c = 0; c < dim; c++) {
                for (int r0 = 0; r0 <= dim - L; r0++) {
                    int base = r0 * dim + c;
                    boolean blocked = false, coversHit = !haveHit;
                    for (int k = 0; k < L; k++) {
                        int id = base + k * dim;
                        byte h = hitMap[id];
                        if (h == 1 || h == 3) { blocked = true; break; }
                        if (h == 2) coversHit = true;
                    }
                    if (blocked || !coversHit) continue;

                    total += copies;
                    for (int k = 0; k < L; k++) {
                        int id = base + k * dim;
                        if (hitMap[id] == 0) coverCount[id] += copies;
                    }
                }
            }
        }
        return total;
    }
}
