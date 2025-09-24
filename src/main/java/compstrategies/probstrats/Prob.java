package compstrategies.probstrats;

import compstrategies.Hunt;

public class Prob {
    protected final int dim, nCells;
    protected final int[] prob;
    protected final byte[] hitMap;   // 0=unknown, 1=miss, 2=hit, 3 for sunk
    protected final int[] remain = new int[6]; // remaining ship lengths
    protected Hunt h;                 // Hunt mode object

    public Prob(int dimension) {
        this.dim = dimension;
        this.nCells = dim * dim;
        this.prob = new int[nCells];
        this.hitMap = new byte[nCells];

        int[] fleet = (dim == 3) ? new int[]{3,3,2} : new int[]{5,4,3,3,2};
        for (int L : fleet) remain[L]++;  // e.g., remain[3] == 2

        recompute();
    }

    public void trackShot(boolean hit, int sunkLen, int pos, int[] sunkCells) {
        hitMap[pos] = hit ? (byte)2 : (byte)1;
        if (hit) {
            if (h == null) {
                h = new Hunt(pos, dim); // start Hunt mode
            }
            h.registerHit(pos, hitMap);
        }
        if (hit && sunkLen > 1 && sunkLen < 6 && remain[sunkLen] > 0 && sunkCells != null) {
            remain[sunkLen]--;
            int limit = Math.min(sunkLen, sunkCells.length);
            for (int k = 0; k < limit; k++) {
                int cid = sunkCells[k];
                hitMap[cid] = 3; // mark sunk cells
            }

            if (h != null) {
                h.reset();
                h = null; // exit hunt mode once ship is sunk
            }
        }
    }

    public int selectShot() {
        int shot = -1;

        // Hunt mode first
        if (h != null) {
            try {
                while (true) {
                    shot = h.huntShip(hitMap);
                    if (shot >= 0 && shot < hitMap.length && hitMap[shot] == 0) {
                        hitMap[shot] = 1; // mark as attempted
                        return shot;
                    }
                }
            } catch (Exception e) {
                h = null; // fallback if Hunt fails
            }
        }

        // Probability mode
        recompute();
        int bestId = -1, bestScore = Integer.MIN_VALUE;
        for (int id = 0; id < nCells; id++) {
            if (hitMap[id] != 0) continue;
            int s = prob[id];
            if (s > bestScore) { bestScore = s; bestId = id; }
        }
        if (bestId < 0) throw new IllegalStateException("No cells left to fire at");
        return bestId; // (row, col)
    }

    protected void recompute() {
        java.util.Arrays.fill(prob, 0);

        for (int L = 2; L <= 5; L++) {
            int copies = remain[L];
            if (copies == 0) continue;

            // vertical placements
            for (int y = 0; y < dim; y++) {
                for (int x = 0; x <= dim - L; x++) {
                    int base = flatten(x,y);
                    boolean blocked = false;
                    for (int p = base, k = 0; k < L; k++, p += dim) {
                        if (hitMap[p] == 1 || hitMap[p] == 3) {blocked = true; break;}
                    }
                    if (!blocked){
                        for (int p = base, k = 0; k < L; k++, p += dim) {
                            if (hitMap[p] == 0) prob[p] += copies;
                        }
                    }
                }
            }

            // horizontal
            for (int x = 0; x < dim; x++) {
                for (int y = 0; y <= dim - L; y++) {
                    int base = flatten(x,y);
                    boolean blocked = false;
                    for (int p = base, k = 0; k < L; k++, p++) {
                        if (hitMap[p] == 1 || hitMap[p] == 3) { blocked = true; break; }
                    }
                    if (!blocked){
                        for (int p = base, k = 0; k < L; k++, p++) {
                            if (hitMap[p] == 0) prob[p] += copies;
                        }
                    }
                }
            }
        }
    }

    private int flatten(int x, int y) { return x * dim + y; }

    public void printProb() {
        for (int x = 0; x < dim; x++) {
            int row = x * dim;
            for (int y = 0; y < dim; y++) {
                System.out.printf("%3d", prob[row + y]);
            }
            System.out.println();
        }
        System.out.println();
    }

    public void printShots(){
        for (int x = 0; x < dim; x++) {
            int row = x * dim;
            for (int y = 0; y < dim; y++) {
                System.out.printf("%3d", hitMap[row + y]);
            }
            System.out.println();
        }
        System.out.println();
    }
}

