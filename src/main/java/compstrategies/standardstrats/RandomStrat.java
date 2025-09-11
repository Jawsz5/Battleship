package compstrategies.standardstrats;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;


public class RandomStrat {
    protected ArrayList<Integer> notShotAtSpots;
    protected int dim, nCells;
    public RandomStrat(int mapDim){
        dim = mapDim;
        nCells = mapDim*mapDim;
        notShotAtSpots = new ArrayList<>(nCells);
        for (int i = 0; i < nCells; i++){
            notShotAtSpots.add(i);
        }
    }
    public int[] selectShot() {
        if (notShotAtSpots.isEmpty()) throw new IllegalStateException("No squares left");
        int idx = ThreadLocalRandom.current().nextInt(notShotAtSpots.size());
        int spot = notShotAtSpots.remove(idx);                              
        int x = spot / dim;
        int y = spot % dim;
        return new int[]{x, y};
    }
    //filler method to maintain consistency with other strats for the game loop
    public void trackShot(boolean hit, int sunkLen, int x, int y){}
    
}
