package app.src.main.GameFiles;
import java.util.Scanner;
import java.io.IOException;
import java.util.ArrayList;
import app.src.main.ComputerStrategies.RandomStrat;


public class Game{
    private int maxTurns, turnsPlayed, dimension;
    private char[][] hitMap;
    //private char[][] grid;
    private Ocean playerOcean;
    private int xShot, yShot;
    private boolean gameWon = false;
    private ArrayList<Ship> boats;

    public Game(int mapSize, int maxNumTurns){
        try{playerOcean = new Ocean(mapSize);}
        catch(Exception e){System.out.println(e);}
        turnsPlayed = 0;
        dimension = mapSize;

        hitMap = new char[mapSize][mapSize];
        //grid = playerOcean.getGrid();
        for(int i = 0; i < mapSize; i++) {
            for(int j = 0; j < mapSize; j++) {
                hitMap[i][j] = '1'; // Initialize the hitMap with zeros
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
        if(hitMap[x][y] != '1'){
            throw new IllegalArgumentException("Spot has already been shot at");
        }
        if(playerOcean.getGrid()[x][y] != 'e'){
            hitMap[x][y] = 'X';
            for(Ship s: boats){
                for(int i = 0; i < s.getSpotsX().length; i++){
                    if(s.getSpotsX()[i] == x && s.getSpotsY()[i] == y){
                        s.getSpotsX()[i] = -1;
                        s.getSpotsY()[i] = -1;
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

            if(playerOcean.isAllSunk()){
                gameWon = true;
                System.out.println("All ships have been sunk!");
            }
            if(gameWon){
                System.out.println("You win!");
                break;
            }
            int boatToRemove = 0;
            boolean isBoatSunk = false;
            for(int m = 0; m < boats.size(); m++){
                if(boats.get(m).isSunk()){
                    System.out.println(boats.get(m).getBoatName() + " is sunk!");
                    boatToRemove = m; 
                    isBoatSunk = true;
                    //only output the message once, boats is a duplicate of the internal ocean boat arraylist so nothing is actually changed
                }
            }
            if(isBoatSunk){boats.remove(boatToRemove);}
            turnsPlayed += 1;
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
    public void playGameComputer() throws IOException{
        RandomStrat r = new RandomStrat(dimension);
        for(int i = 0; i < dimension*dimension + 1; i++){
            if(playerOcean.isAllSunk()){gameWon = true;break;}
            turnsPlayed += 1;
            int[] shot = new int[2];
            shot = r.selectShot();
            shoot(shot[0], shot[1]);
        }
        if(!gameWon){throw new IOException("Strategy failed to complete the game");} 
        //due to shoot mechanic that prevents refires on the same square, this should never happen
    }
    public int getTurnsPlayed(){
        return turnsPlayed;
    }

    
}
