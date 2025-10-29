package compstrategies.probstrats;

import compstrategies.Hunt;

public class CellMaxProb {
    protected final int dim, nCells;
    protected final int[] prob;
    protected final byte[] hitMap;   // 0=unknown, 1=miss, 2=hit, 3 for sunk
    protected final int[] remain = new int[6]; // remaining ship lengths + an extra space
    protected Hunt h;                 // Hunt mode object
    public CellMaxProb(int dimension){
        this.dim = dimension;
        this.nCells = dim * dim;
        this.prob = new int[nCells];
        this.hitMap = new byte[nCells];

        int[] fleet = (dim == 3) ? new int[]{3,3,2} : new int[]{5,4,3,3,2};
        // uses the index of remain to store #ships at a given length
        for (int L : fleet) remain[L]++;  // e.g., remain[3] == 2

        recompute();
    }
    public void recompute(){
        java.util.Arrays.fill(prob, 0);
        for(int L = 2; L <= 5; L++){
            int copies = remain[L];
            if(copies == 0) continue;
            //vertical ship placements
            for(int b = 0; b < nCells - dim*L; b++){
                boolean blocked = false;
                for (int p = b, k = 0; k < L; k++, p += dim) {
                    if (hitMap[p] == 1 || hitMap[p] == 3) {blocked = true; break;}
                }
                if(!blocked){
                    //TODO swtich this logic to add the length of each ship to the base
                    for (int p = b, k = 0; k < L; k++, p += dim) {
                        if (hitMap[p] == 0) prob[p] += copies;
                    }
                }
            }
            //horizontal placements
            for(int p = 0; p < nCells - dim*L; p++){
                
            }
        }

    }
}
