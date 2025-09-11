package compstrategies.standardstrats;
import java.util.concurrent.ThreadLocalRandom;

import compstrategies.Hunt;

public class RandomHuntStrat extends RandomStrat{
    private int prevShot;
    private int[] hitMap;
    private int[] hitOneSurround = new int[]{10,-10,1,-1};
    private boolean sunk = false, newHunt;
    private Hunt h;
    public RandomHuntStrat(int mapDim){
        super(mapDim);
        prevShot = -1;
        hitMap = new int[nCells];
        for(int i = 0; i < hitMap.length; i++){
            hitMap[i] = 0;
        }
    }
    public void trackShot(boolean hit, int sunkLen, int x, int y){
        prevShot = flatten(x,y);
        sunk = (sunkLen != 0) ? true: false;
        hitMap[prevShot] = hit ? 2: 1;
    }
    private int flatten(int x, int y){return 10*x + y;}

    public int[] selectShot(){
        if(notShotAtSpots.isEmpty()) throw new IllegalStateException("No squares left");
        if(sunk){newHunt = true;} //only reset the hunt when a ship is sunk
        if(prevShot != -1){
            if(hitMap[prevShot] == 2 && newHunt){
                h = new Hunt(prevShot); //only initialize a new hunt when 1 square on the boat is hit
                newHunt = false;
                for(int i: hitOneSurround){
                    i = (hitMap[prevShot + i] == 2) ? 3: hitMap[prevShot];
                }
            }
        
            if(hitMap[prevShot] == 2){
                //rest of the hunt
                for(int i:hitOneSurround){
                    if (i == 2){h.setTargetted(false);}
                }
                int shot = h.huntShip(hitMap);
                return new int[]{shot/dim, shot%dim};
            }
        }
        int idx  = ThreadLocalRandom.current().nextInt(notShotAtSpots.size());
        prevShot = idx;
        int shot = notShotAtSpots.remove(idx);
        int x = shot / dim;
        int y = shot % dim;
        return new int[]{x, y};
    }
}
