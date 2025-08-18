package ComputerStrategies;
import GameFiles.Ocean;
import GameFiles.Ship;
import java.util.ArrayList;

public class RandomHuntStrat extends RandomStrat {
    private Ocean hitTracker;
    private char[][] grid;
    private int[] previousShot;
    private ArrayList<Ship> boats;
    private ArrayList<int[]> candidates;

    public RandomHuntStrat(int mapSize, Ocean o){
        super(mapSize);
        hitTracker = o;
        grid = hitTracker.getGrid();
        boats = hitTracker.getBoats();
        previousShot = super.selectShot();
        candidates = new ArrayList<>();
    }
    
    private void addCandidates(int x, int y){
        int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for(int[] d : directions){
            int candidateX = x + d[0];
            int candidateY = y + d[1];
            if(candidateX >= 0 && candidateX < hitTracker.getDimension() &&
               candidateY >= 0 && candidateY < hitTracker.getDimension() &&
               isAvailable(candidateX, candidateY)) {
                candidates.add(new int[]{candidateX, candidateY});
            }
        }
    }

    @Override
    public int[] selectShot() {
        int x = previousShot[0];
        int y = previousShot[1];

        if(hitTracker.isHit(x, y)){
            for(Ship b : boats){
                if(b.isHit(x, y)){
                    if(b.isSunk()){
                        candidates.clear();
                    } else {
                        addCandidates(x, y);
                    }
                    break;
                }
            }
        }   

        while(!candidates.isEmpty()){
            int[] candidate = candidates.remove(0);
            if(isAvailable(candidate[0], candidate[1])){
                previousShot = candidate;
                return previousShot;
            }
        }

        previousShot = super.selectShot();
        return previousShot;
    }
}
