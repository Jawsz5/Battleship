package compstrategies.probstrats;


//experimenting to speed up the probability map
public class EfficiencyExpProb {
    private final int nCells, dim, nCandidates;
    //cell start stores the starting index of each cell in the ship id list. Cell length, stores number of ships per cell
    private int cellStart[], cellLength[], shipTypeStart[];
    private short[] cellIDs; //cell id's
    private byte[] candLength; //candidate length
    private short[] candCellsOcc; //candidate cells occupied
    private java.util.BitSet alive;
    public EfficiencyExpProb(int dimension){
        dim = dimension; nCells = dim*dim;
        int[] shipSizes = (dim == 3) ? new int[]{3, 3, 2}: new int[]{5,4,3,3,2};
        int maxShipSize = shipSizes[0]; //largest ship size is the 0th element
        nCandidates = numCandidates(dim, shipSizes);

        candLength = new byte[nCandidates]; //array that stores the lengths of each candidate
        candCellsOcc = new short[maxShipSize*nCandidates];//array that stores all cells that a candidate occupies max of 5
        java.util.Arrays.fill(candCellsOcc, (short)-1);

        //calcuclate the number of ships per cell
        cellLength = new int[nCells];
        int cid = 0;
        shipTypeStart = new int[shipSizes.length + 1];
        for(int m = 0; m < shipSizes.length; m++){
            int s = shipSizes[m];
            shipTypeStart[m] = cid;
            //horizontal ships
            for(int y = 0; y < dimension; y++){
                for(int x = 0; x <= dimension - s; x++, cid++){
                    candLength[cid] = (byte) s;
                    for(int i = 0; i < s; i++){
                        cellLength[flaten(x+i,y)]++;
                    }
                }
            }
            // vertical
            for(int x = 0; x < dimension; x++){
                for(int y = 0; y <= dimension - s; y++, cid++){
                    candLength[cid] = (byte) s;
                    for(int i = 0; i < s; i++){
                        cellLength[flaten(x,y+i)]++;
                    }
                }
            }
        }
        shipTypeStart[shipSizes.length] = nCandidates;

        //build the CSR
        cellStart = new int[nCells + 1];
        cellStart[0] = 0;
        for(int i = 0; i < nCells; i++){
            cellStart[i+1] = cellStart[i] + cellLength[i];
        }
        cellIDs = new short[cellStart[nCells]];
        int[] cursors = cellStart.clone();
        //add candidates to their respective cells, add cells to the cell id's
        cid = 0;
        for(int s: shipSizes){
            //horizontal ships
            for(int y = 0; y < dimension; y++){
                for(int x = 0; x <= dimension - s; x++, cid++){
                    for(int i = 0; i < s; i++){
                        int cell = flaten(x+i, y);
                        candCellsOcc[cid*maxShipSize + i] = (short) cell;
                        cellIDs[cursors[cell]++] = (short) cid;
                    }
                }
            }
            // vertical
            for(int x = 0; x < dimension; x++){
                for(int y = 0; y <= dimension - s; y++, cid++){
                    for(int i = 0; i < s; i++){
                        int cell = flaten(x, y+i);
                        candCellsOcc[cid*maxShipSize + i] = (short) cell;
                        cellIDs[cursors[cell]++] = (short) cid;
                    }
                }
            }
        }
        alive = new java.util.BitSet(nCandidates);
        alive.set(0, nCandidates); // all candidates start alive

    }
    private int flaten(int x, int y){return dim * y + x;} //flaten the 2-d coordinates to 1-d for speed and to match the dimension of cells
    private int numCandidates(int dimension, int[] shipSizes){
        int count = 0;
        for(int i: shipSizes){
            count += 2 * dimension * (dimension - i + 1);
        }
        return count;
    }

    private final java.util.BitSet fired = new java.util.BitSet(); // cells already shot

    public void trackShot(boolean hit, int sunkLen, int pos, int[] sunkCells) {
        //System.out.println(hit + " " + sunkLen + "  " + x+","+y);
        fired.set(pos); // never pick this cell again

        // MISS: kill all candidates that cover (x,y)
        if (hit == false) {
            int start = cellStart[pos], end = cellStart[pos + 1];
            for (int i = start; i < end; i++) {
                int cid = cellIDs[i] & 0xFFFF;
                alive.clear(cid);
            }
            return;
        }

        // Sunk boat remove the entire block for that ship length from the grid using shipTypeStart
        if(sunkLen > 0){
            for (int t = 0; t < shipTypeStart.length - 1; t++) {
                int a = shipTypeStart[t], b = shipTypeStart[t + 1];
                if ((candLength[a] & 0xFF) != sunkLen) continue;

                // skip if already removed
                if (alive.nextSetBit(a) >= b) continue;

                // clear all candidates of this sunk type (fast range clear)
                alive.clear(a, b);
                break; // only one ship of that length sunk
            }
        }
    }

    // helper to count alive candidates covering a cell (≤34 checks)
    private int aliveCountAtCell(int cell) {
        int s = cellStart[cell], e = cellStart[cell + 1], c = 0;
        for (int i = s; i < e; i++) {
            int cid = cellIDs[i] & 0xFFFF;
            if (alive.get(cid)) c++;
        }
        return c;
    }

    // choose the best shot (x,y) based on current alive counts; skip fired cells
    public int selectShot() {
        int bestCell = -1, bestScore = Integer.MIN_VALUE;
        for (int cell = 0; cell < nCells; cell++) {
            if (fired.get(cell)) continue;       
            int score = aliveCountAtCell(cell);
            if (score > bestScore) { bestScore = score; bestCell = cell; }
        }
        if (bestCell < 0) throw new IllegalStateException("No cells left");

        
        //System.out.println(bestCell%dim + " " + bestCell/dim);
        for(int i = 0; i < dim; i++){
            for(int j = 0; j < dim; j++){
                //System.out.print(aliveCountAtCell(flaten(i,j)) + " ");
            }
            //System.out.println("\n");
        }
        //System.out.println("\n\n\n\n");
        
        // invert flaten(x,y) = y*dim + x; for future reference, x and y are flipped in this project as y is column, x is row

        return bestCell;
    }


}