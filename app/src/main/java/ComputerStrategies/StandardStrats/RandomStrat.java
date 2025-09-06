import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;


public class RandomStrat {
   private ArrayList<Integer> squares;
   private int dimension, size;
   public RandomStrat(int mapSize){
       dimension = mapSize;
       size = mapSize*mapSize;
       squares = new ArrayList<>(size);
       for (int i = 0; i < size; i++){
           squares.add(i);
       }
   }
   public boolean isAvailable(int x, int y){
    int spot = y * dimension + x;   // same encoding as in selectShot
    return squares.contains(spot);
}

    public void removeSquare(int x, int y){
        squares.remove(y * dimension + x);
    }

   
   public int[] selectShot() {
       if (squares.isEmpty()) throw new IllegalStateException("No squares left");
       int idx = ThreadLocalRandom.current().nextInt(squares.size());
       int spot = squares.remove(idx);                              
       int x = spot % dimension;
       int y = spot / dimension;
       return new int[]{x, y};
   }
   public void trackShot(boolean hit, int sunkLen, int x, int y){
        return; //filler method to maintain consistency with other methods for the game loop
   }
}
