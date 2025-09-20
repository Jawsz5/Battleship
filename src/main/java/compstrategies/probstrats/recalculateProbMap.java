package compstrategies.probstrats;


public final class recalculateProbMap {
    private final int dim, nCells;
    private final int[] prob;
    private final byte[] hitMap;   // 0=unknown, 1=miss, 2=hit, 3 for all hits on a sunk boat
    private final int[] remain = new int[6]; // reamining ship lengths, used to keep track of sunk ships

    public recalculateProbMap(int dimension) {
        this.dim = dimension;
        this.nCells = dim * dim;
        this.prob = new int[nCells];
        this.hitMap = new byte[nCells];

        int[] fleet = (dim == 3) ? new int[]{3,3,2} : new int[]{5,4,3,3,2};
        for (int L : fleet) remain[L]++;  // e.g., remain[3] == 2

        recompute();
    }



    public void trackShot(boolean hit, int sunkLen, int x, int y, int[] sunkCells) {
        int id = flatten(x, y);
        hitMap[id] = hit ? (byte)2 : (byte)1;
    
        if (hit && sunkLen > 1 && sunkLen < 6 && remain[sunkLen] > 0 && sunkCells != null) {
            remain[sunkLen]--;
            int limit = Math.min(sunkLen, sunkCells.length);   // <-- use only the actual sunk length
            for (int k = 0; k < limit; k++) {
                int cid = sunkCells[k];
                hitMap[cid] = 3; // block only valid ids
            }
        }
    }
    

    public int[] selectShot() {
        recompute();
        int bestId = -1, bestScore = Integer.MIN_VALUE;
        for (int id = 0; id < nCells; id++) {
            if (hitMap[id] != 0) continue;
            int s = prob[id];
            if (s > bestScore) { bestScore = s; bestId = id; }
        }
        if (bestId < 0) throw new IllegalStateException("No cells left to fire at");
        return new int[]{ bestId / dim, bestId % dim }; // (row, col)
    }    
    

    private void recompute() {
        java.util.Arrays.fill(prob, 0);

        for (int L = 2; L <= 5; L++) {
            int copies = remain[L];

            // vertical placements
            for (int y = 0; y < dim; y++) {
                for (int x = 0; x <= dim - L; x++) {
                    int base = flatten(x,y);
                    // check if any miss in the L cells vertically
                    boolean blocked = false;
                    for (int p = base, k = 0; k < L; k++, p += dim) {
                        if (hitMap[p] == 1 || hitMap[p] == 3) {blocked = true; break;}
                    }
                    // add score to unknown cells only
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

    //convert 2d coordinates to 1d for speed improvment
    private int flatten(int x, int y) { return x * dim + y; }

    // for visual/debugging purposes
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
