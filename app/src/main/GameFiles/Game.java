package app.src.main.GameFiles;
import app.src.main.GameFiles.Ocean;;

public class Game{
    private int turnsPlayed, maxTurns;
    private int[][] hitMap;
    private Ocean playerOcean;
    public Game(int mapSize, int maxNumTurns){
        try{playerOcean = new Ocean(mapSize);}
        catch(Exception e){System.out.println(e);}

        hitMap = new int[mapSize][mapSize];
        for(int i = 0; i < mapSize; i++) {
            for(int j = 0; j < mapSize; j++) {
                hitMap[i][j] = 0; // Initialize the hitMap with zeros
            }
        }
        turnsPlayed = 0;
        if(maxNumTurns > mapSize * 10){throw new IllegalArgumentException("Max number of turns for the game must be less than 10x the dimension of the map");}
        maxTurns = maxNumTurns;
    }
    private void shoot(int x, int y){
        
    }

    
}
