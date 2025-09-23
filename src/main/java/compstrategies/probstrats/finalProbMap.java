package compstrategies.probstrats;

import java.util.*;

public class finalProbMap extends recalculateProbMap {

    public finalProbMap(int dimension) {
        super(dimension);
    }

    @Override
    protected void recompute() {
        Arrays.fill(prob, 0);

        // Collect current hit cells
        List<Integer> hits = new ArrayList<>();
        for (int i = 0; i < hitMap.length; i++) {
            if (hitMap[i] == 2) hits.add(i);
        }

        for (int L = 2; L <= 5; L++) {
            int copies = remain[L];
            if (copies == 0) continue;

            // Horizontal placements
            for (int r = 0; r < dim; r++) {
                for (int c = 0; c <= dim - L; c++) {
                    int base = flatten(r, c);

                    // Validate placement
                    boolean blocked = false, coversHit = hits.isEmpty();
                    for (int k = 0; k < L; k++) {
                        int id = base + k;
                        if (hitMap[id] == 1 || hitMap[id] == 3) {
                            blocked = true;
                            break;
                        }
                        if (hitMap[id] == 2) coversHit = true;
                    }
                    if (blocked || !coversHit) continue;

                    // Add score
                    for (int k = 0; k < L; k++) {
                        int id = base + k;
                        if (hitMap[id] == 0) prob[id] += copies;
                        if (hitMap[id] == 2) prob[id] += copies * 2; // bonus for extending a hit
                    }
                }
            }

            // Vertical placements
            for (int c = 0; c < dim; c++) {
                for (int r = 0; r <= dim - L; r++) {
                    int base = flatten(r, c);

                    boolean blocked = false, coversHit = hits.isEmpty();
                    for (int k = 0; k < L; k++) {
                        int id = base + k * dim;
                        if (hitMap[id] == 1 || hitMap[id] == 3) {
                            blocked = true;
                            break;
                        }
                        if (hitMap[id] == 2) coversHit = true;
                    }
                    if (blocked || !coversHit) continue;

                    for (int k = 0; k < L; k++) {
                        int id = base + k * dim;
                        if (hitMap[id] == 0) prob[id] += copies;
                        if (hitMap[id] == 2) prob[id] += copies * 2;
                    }
                }
            }
        }
    }

    private int flatten(int row, int col) {
        return row * dim + col;
    }
}

    
