package compstrategies.probstrats;

public class CellMaxProb extends ShipMaxProb{
    public CellMaxProb(int dimension){
        super(dimension);
    }
    @Override
    public void recompute(){
        java.util.Arrays.fill(prob, 0);
        for(int L = 2; L <= 5; L++){
            int copies = remain[L];
            if(copies == 0) continue;
            //vertical ship placements
            for(int b = 0; b <= nCells - dim*L; b++){
                boolean blocked = false;
                for (int p = b, k = 0; k < L; k++, p += dim) {
                    if (hitMap[p] == 1 || hitMap[p] == 3) {blocked = true; break;}
                }
                if (!blocked) {
                    for (int p = b, k = 0; k < L; k++, p += dim) {
                        if (hitMap[p] != 0) continue;            // skip known cells
                        for (int q = b, t = 0; t < L; t++, q += dim) {
                            if (hitMap[q] == 0) prob[q] += 1;    // spread influence
                        }
                    }
                }

            }
            //horizontal placements
            for (int y = 0; y < dim; y++) {
                int rowStart = y * dim;
                for (int x = 0; x <= dim - L; x++) {
                    int start = rowStart + x;
                    boolean blocked = false;
                    for (int k = 0, p = start; k < L; k++, p++) {
                        if (hitMap[p] == 1 || hitMap[p] == 3) { blocked = true; break; }
                    }
                    if (!blocked) {
                        for (int p = start, k = 0; k < L; k++, p ++) {
                            if (hitMap[p] != 0) continue;            // skip known cells
                            for (int q = start, t = 0; t < L; t++, q ++) {
                                if (hitMap[q] == 0) prob[q] += 1;    // spread influence
                            }
                        }
                    }
                }
            }
        }

    }
}
