package ComputerStrategies;
import GameFiles.Ocean;
import GameFiles.Ship;
import java.util.ArrayList;

public class RandomHuntStrat extends RandomStrat {
    private Ocean hitTracker;
    private int[] previousShot;
    private ArrayList<Ship> boats;
    private ArrayList<int[]> candidates;

    public RandomHuntStrat(int mapSize, Ocean o){
        super(mapSize);
        hitTracker = o;
        boats = hitTracker.getBoats();
        previousShot = super.selectShot();
        //System.out.println(previousShot[0] + " and " + previousShot[1]);
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
                //removeSquare(candidate[0], candidate[1]);
                //System.out.println(previousShot[0] + " and " + previousShot[1]);
                return previousShot;
            }
        }

        previousShot = super.selectShot();
        if(isAvailable(previousShot[0], previousShot[1])){
             //System.out.println(previousShot[0] + " and " + previousShot[1]);
            return previousShot;

        }
        //System.out.println("hi");
        return new int[]{0,7};
       
    }
}
