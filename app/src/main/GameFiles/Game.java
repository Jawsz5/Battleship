package app.src.main.GameFiles;
import java.util.Scanner;
import java.util.ArrayList;


public class Game{
    private int maxTurns;
    private char[][] hitMap;
    //private char[][] grid;
    private Ocean playerOcean;
    private int xShot, yShot;
    private boolean gameWon = false;
    private ArrayList<Ship> boats;

    public Game(int mapSize, int maxNumTurns){
        try{playerOcean = new Ocean(mapSize);}
        catch(Exception e){System.out.println(e);}

        hitMap = new char[mapSize][mapSize];
        //grid = playerOcean.getGrid();
        for(int i = 0; i < mapSize; i++) {
            for(int j = 0; j < mapSize; j++) {
                hitMap[i][j] = 'm'; // Initialize the hitMap with zeros
            }
        }
        if(maxNumTurns > mapSize * 10){throw new IllegalArgumentException("Max number of turns for the game must be less than 10x the dimension of the map");}
        maxTurns = maxNumTurns;
        playerOcean.placeRandomBoats();
        boats = playerOcean.getBoats();
    }
    private void shoot(int x, int y) throws IllegalArgumentException{
        if(x < 0 || x > 9){
            throw new IllegalArgumentException("X-coordinate is out of bounds");
        }
        if(y < 0 || y > 9){
            throw new IllegalArgumentException("Y-coordinate is out of bounds");
        }
        if(playerOcean.getGrid()[x][y] != 'e'){
            hitMap[x][y] = 'X';
            for(Ship s: boats){
                for(int k: s.getSpotsX()){
                    if(k == x){
                        k = -1;
                    }
                }
                for(int k: s.getSpotsY()){
                    if(k == x){
                        k = -1;
                    }
                }
            }
        } else{hitMap[x][y] = 'O';}
    }
    public void playGameOnTerminal_Human(){
        Scanner shotInput = new Scanner(System.in);

        for(int i = 0; i < maxTurns; i++){
            //print out the board
            for(int j = 0; j < playerOcean.getDimension(); j++){
                for(int k = 0; k < playerOcean.getDimension(); k++){
                    System.out.print(hitMap[j][k] + " ");
                }
                System.out.println("\n");
            }
            System.out.println("\n\n\n");
            /*for(int j = 0; j < playerOcean.getDimension(); j++){
                for(int k = 0; k < playerOcean.getDimension(); k++){. //Used for testing purposes
                    System.out.print(grid[j][k] + " ");
                }
                System.out.println();
            }*/
            if(playerOcean.isAllSunk()){
                gameWon = true;
                System.out.println("All ships have been sunk!");
            }
            if(gameWon){
                System.out.println("You win!");
                break;
            }
            for(Ship boat: boats){
                if(boat.isSunk()){
                    System.out.println(boat.getBoatType() + " is sunk!");
                    boats.remove(boat); 
                    //only output the message once, boats is a duplicate of the internal ocean boat arraylist so nothing is actually changed
                }
            }

            //prompt the user for a shot
            boolean isShotFired = false;
            do{
                try{
                    System.out.print("enter an x coordinate to shoot: ");
                    xShot = shotInput.nextInt();
                    System.out.print("enter a y coordinate to shoot: ");
                    yShot = shotInput.nextInt();
                    shoot(xShot, yShot);
                    isShotFired = true;
                }catch(IllegalArgumentException e){System.out.println(e);}
            }while(!isShotFired);
            
        }
        if(!gameWon){System.out.println("You Lose :(");}
        shotInput.close();
    }

    
}
