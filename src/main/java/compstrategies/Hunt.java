package compstrategies;


import java.util.ArrayList;
import java.util.List;


/**
* Hunt mode: triggered when a hit is found.
* This class tries to extend in both directions along
* a line until the ship is sunk.
*/
public class Hunt {
   private int dim;
   private int origin;             // first hit cell
   private List<Integer> frontier; // candidate squares
   private boolean oriented;       // true once direction is determined


   public Hunt(int startPos, int dim) {
       this.dim = dim;
       this.origin = startPos;
       this.frontier = new ArrayList<>();
       this.oriented = false;


       // start with neighbors of the first hit
       addNeighbors(startPos);
   }


   private void addNeighbors(int pos) {
       int row = pos / dim;
       int col = pos % dim;


       if (row > 0) frontier.add((row - 1) * dim + col);
       if (row < dim - 1) frontier.add((row + 1) * dim + col);
       if (col > 0) frontier.add(row * dim + (col - 1));
       if (col < dim - 1) frontier.add(row * dim + (col + 1));
   }


   /** Reset when ship is sunk */
   public void reset() {
       frontier.clear();
       oriented = false;
   }


   /**
    * Choose next shot in Hunt mode.
    * Extends in both directions when orientation is known.
    */
   public int huntShip(byte[] hitMap) throws Exception {
       if (frontier.isEmpty()) throw new Exception("No hunt candidates");


       // Pick the next candidate
       int pos = frontier.remove(0);


       // Skip if already shot
       if (hitMap[pos] != 0) return huntShip(hitMap);


       return pos;
   }


   /**
    * After a hit is confirmed, call this to extend hunt.
    */
   public void registerHit(int pos, byte[] hitMap) {
       int row = pos / dim;
       int col = pos % dim;


       // Determine orientation if possible
       if (!oriented) {
           if (Math.abs((origin / dim) - row) == 1 && (origin % dim) == col) {
               oriented = true; // vertical
               extendVertical(origin, hitMap);
               extendVertical(pos, hitMap);
           } else if (Math.abs((origin % dim) - col) == 1 && (origin / dim) == row) {
               oriented = true; // horizontal
               extendHorizontal(origin, hitMap);
               extendHorizontal(pos, hitMap);
           } else {
               // still unknown: just add neighbors
               addNeighbors(pos);
           }
       } else {
           // orientation known, extend along that line
           if ((origin / dim) == row) {
               extendHorizontal(pos, hitMap);
           } else if ((origin % dim) == col) {
               extendVertical(pos, hitMap);
           }
       }
   }


   private void extendHorizontal(int pos, byte[] hitMap) {
       int row = pos / dim;
       int col = pos % dim;


       // extend left
       for (int c = col - 1; c >= 0; c--) {
           int idx = row * dim + c;
           if (hitMap[idx] == 1) break; // miss encountered
           if (hitMap[idx] == 0) frontier.add(idx);
           if (hitMap[idx] == 2) continue; // already hit, keep going
       }


       // extend right
       for (int c = col + 1; c < dim; c++) {
           int idx = row * dim + c;
           if (hitMap[idx] == 1) break;
           if (hitMap[idx] == 0) frontier.add(idx);
           if (hitMap[idx] == 2) continue;
       }
   }


   private void extendVertical(int pos, byte[] hitMap) {
       int row = pos / dim;
       int col = pos % dim;


       // extend up
       for (int r = row - 1; r >= 0; r--) {
           int idx = r * dim + col;
           if (hitMap[idx] == 1) break;
           if (hitMap[idx] == 0) frontier.add(idx);
           if (hitMap[idx] == 2) continue;
       }


       // extend down
       for (int r = row + 1; r < dim; r++) {
           int idx = r * dim + col;
           if (hitMap[idx] == 1) break;
           if (hitMap[idx] == 0) frontier.add(idx);
           if (hitMap[idx] == 2) continue;
       }
   }
}
