package app.src.main.GameFiles;
import java.util.Scanner;
import java.util.ArrayList;


public class Game{
    private int maxTurns;
    private char[][] hitMap;
    private Ocean playerOcean;
    private int xShot, yShot;
    private boolean gameWon = false;
    private ArrayList<Ship> boats;

    public Game(int mapSize, int maxNumTurns){
        try{playerOcean = new Ocean(mapSize);}
        catch(Exception e){System.out.println(e);}

        hitMap = new char[mapSize][mapSize];
        for(int i = 0; i < mapSize; i++) {
            for(int j = 0; j < mapSize; j++) {
                hitMap[i][j] = 'm'; // Initialize the hitMap with zeros
            }
        }
        if(maxNumTurns > mapSize * 10){throw new IllegalArgumentException("Max number of turns for the game must be less than 10x the dimension of the map");}
        maxTurns = maxNumTurns;

        boats = playerOcean.getBoats();
    }
    private void shoot(int x, int y){
        if(playerOcean.getGrid()[x][y] != 'e'){
            hitMap[x][y] = 'X';
        } else{hitMap[x][y] = 'O';}
    }
    public void playGameOnTerminal_Human(){
        Scanner shotInput = new Scanner(System.in);

        for(int i = 0; i < maxTurns; i++){
            //print out the board
            for(int j = 0; j < playerOcean.getDimension(); j++){
                for(int k = 0; k < playerOcean.getDimension(); k++){
                    System.out.print(hitMap[i][j] + " ");
                }
                System.out.println("\n");
            }
            System.out.println("\n\n\n");

            //ask for a shot
            System.out.print("enter an x coordinate to shoot: ");
            xShot = shotInput.nextInt();
            System.out.print("enter a y coordinate to shoot: ");
            yShot = shotInput.nextInt();
            shoot(xShot, yShot);
            shotInput.close();
            if(gameWon){break;}
        }
        System.out.println("You Lose :(");
    }

    
}
