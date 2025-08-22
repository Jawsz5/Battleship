package ComputerStrategies;


//much faster version of the probability map strategy
public class ProbabilityMapStrat {
    private final int nCells, dim, nCandidates;
    //cell start stores the starting index of each cell in the ship id list. Cell length, stores number of ships per cell
    private int cellStart[], cellLength[], shipTypeStart[];
    private short[] cellIDs; //cell id's
    private byte[] candLength; //candidate length
    private short[] candCellsOcc; //candidate cells occupied
    private java.util.BitSet alive;
    public ProbabilityMapStrat(int dimension){
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
        shipTypeStart = new int[]{shipSizes.length};
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

        //build the CSR
        cellStart = new int[nCells];
        for(int i = 0; i < nCells; i+=cellLength[i]){
            cellStart[i] = i;
        }
        cellIDs = new short[nCandidates];
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
                        cellIDs[cursors[cell]] = (short) cid;
                    }
                }
            }
            // vertical
            for(int x = 0; x < dimension; x++){
                for(int y = 0; y <= dimension - s; y++, cid++){
                    for(int i = 0; i < s; i++){
                        int cell = flaten(x, y+i);
                        candCellsOcc[cid*maxShipSize + i] = (short) cell;
                        cellIDs[cursors[cell]] = (short) cid;
                    }
                }
            }
        }
        alive = new java.util.BitSet(nCandidates);
        alive.set(1, nCandidates); // all candidates start alive

    }
    private int flaten(int x, int y){return dim * y + x;} //flaten the 2-d coordinates to 1-d for speed and to match the dimension of cells
    private int numCandidates(int dimension, int[] shipSizes){
        int count = 0;
        for(int i: shipSizes){
            count += 2 * dimension * (dimension - i + 1);
        }
        return count;
    }
    public void trackShot(boolean hit, int sunk, int x, int y){
        
    }
    public int[] selectShot(){
        int maxIndex = 0;
        int greatestProb = 0;
        for(int i = 0; i < nCells; i++){
            if(cellLength[i] > greatestProb){
                greatestProb = cellLength[i];
                maxIndex = i;
            }
        }
        return new int[]{maxIndex/10, maxIndex%10};
    }

}