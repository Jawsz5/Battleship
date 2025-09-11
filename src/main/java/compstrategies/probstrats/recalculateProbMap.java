package compstrategies.probstrats;


public final class recalculateProbMap {
    private final int dim, nCells;
    private final int[] prob;
    private final byte[] state;   // 0=unknown, 1=miss, 2=hit 
    private final int[] remain = new int[6]; // reamining ship lengths, used to keep track of sunk ships

    public recalculateProbMap(int dimension) {
        this.dim = dimension;
        this.nCells = dim * dim;
        this.prob = new int[nCells];
        this.state = new byte[nCells];

        int[] fleet = (dim == 3) ? new int[]{3,3,2} : new int[]{5,4,3,3,2};
        for (int L : fleet) remain[L]++;  // e.g., remain[3] == 2

        recompute();
    }



    public void trackShot(boolean hit, int sunkLen, int x, int y) {
        int id = flatten(x, y);
        state[id] = hit ? (byte)2 : (byte)1;
        if (sunkLen >= 2 && sunkLen <= 5 && remain[sunkLen] > 0) {
            remain[sunkLen]--;  // actually remove one ship of that length
        }
    }

    public int[] selectShot() {
        recompute();

        int bestId = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int id = 0; id < nCells; id++) {
            if (state[id] != 0) continue;  // don't re-shoot
            int s = prob[id];
            if (s > bestScore) { bestScore = s; bestId = id; }
        }
        if (bestId < 0) throw new IllegalStateException("No cells left to fire at");
        return new int[]{ bestId % dim, bestId / dim };
    }

    private void recompute() {
        java.util.Arrays.fill(prob, 0);

        for (int L = 2; L <= 5; L++) {
            int copies = remain[L];

            // vertical placements
            for (int x = 0; x < dim; x++) {
                for (int y = 0; y <= dim - L; y++) {
                    int base = flatten(x, y);
                    // check if any miss in the L cells vertically
                    boolean blocked = false;
                    for (int p = base, k = 0; k < L; k++, p += dim) {
                        if (state[p] == 1) { blocked = true; break; }
                    }
                    if (blocked) continue;
                    // add score to unknown cells only
                    for (int p = base, k = 0; k < L; k++, p += dim) {
                        if (state[p] == 0) prob[p] += copies;
                    }
                }
            }

            // horizontal
            for (int y = 0; y < dim; y++) {
                int rowStart = y * dim;
                for (int x = 0; x <= dim - L; x++) {
                    int base = rowStart + x;
                    boolean blocked = false;
                    for (int p = base, k = 0; k < L; k++, p++) {
                        if (state[p] == 1) { blocked = true; break; }
                    }
                    if (blocked) continue;
                    for (int p = base, k = 0; k < L; k++, p++) {
                        if (state[p] == 0) prob[p] += copies;
                    }
                }
            }
        }
    }

    //convert 2d coordinates to 1d for speed improvment
    private int flatten(int x, int y) { return y * dim + x; }

    // for visual/debugging purposes
    public void printProb() {
        for (int y = 0; y < dim; y++) {
            int row = y * dim;
            for (int x = 0; x < dim; x++) {
                System.out.printf("%3d", prob[row + x]);
            }
            System.out.println();
        }
        System.out.println();
    }
}
