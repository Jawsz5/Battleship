package ComputerStrategies.StandardStrats;

import java.util.*;

import GameFiles.Ocean;
import GameFiles.Ship;


public class CheckerBoardHuntStrat {
   protected int dimension;
   protected Ocean ocean;
   protected ArrayList<int[]> huntSquares;
   protected Queue<int[]> targetQueue;
   protected boolean[][] fired;
   protected boolean hitBoat;


   public CheckerBoardHuntStrat(int dim, Ocean ocean) {
       dimension = dim;
       this.ocean = ocean;
       huntSquares = new ArrayList<>();
       targetQueue = new LinkedList<>();
       fired = new boolean[dim][dim];
       hitBoat = false;


       // Fill hunt squares in checkerboard pattern
       for (int x = 0; x < dim; x++) {
           for (int y = 0; y < dim; y++) {
               if ((x + y) % 2 == 0) {
                   huntSquares.add(new int[]{x, y});
               }
           }
       }
       Collections.shuffle(huntSquares);
   }


   // Add neighboring squares to target queue if valid
   private void addCandidates(int x, int y) {
       int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
       for (int[] d : dirs) {
           int nx = x + d[0];
           int ny = y + d[1];
           if (nx >= 0 && nx < dimension && ny >= 0 && ny < dimension && !fired[nx][ny]) {
               targetQueue.add(new int[]{nx, ny});
           }
       }
   }


   // Select the next shot
   public int[] selectShot() {
       int[] shot;

       while (true) {
           if (!targetQueue.isEmpty()) {
               shot = targetQueue.poll();
           } else {
               if (huntSquares.isEmpty()) {
                   throw new IllegalStateException("No hunt squares left");
               }
               shot = huntSquares.remove(huntSquares.size() - 1);
           }


           int x = shot[0];
           int y = shot[1];


           if (fired[x][y]) continue;


           fired[x][y] = true;


           // If hit, add candidates and check for sunk ship
           if (hitBoat) {
               addCandidates(x, y);


               for (Ship ship : ocean.getBoats()) {
                   int[] shipX = ship.getSpotsX();
                   int[] shipY = ship.getSpotsY();
                   for (int i = 0; i < shipX.length; i++) {
                       if (shipX[i] == x && shipY[i] == y && ship.isSunk()) {
                           targetQueue.clear();
                           break;
                       }
                   }
               }
           }


           return shot;
       }
   }
   public void trackShot(boolean hit, int sunkLen, int x, int y){
    if(hit){hitBoat = true;}else{hitBoat = false;}
}
}
