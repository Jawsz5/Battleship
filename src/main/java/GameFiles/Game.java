package gamefiles;

import java.util.Scanner;

import compstrategies.Parity;
import compstrategies.probstrats.EfficiencyExpProb;
import compstrategies.probstrats.Prob;
import compstrategies.probstrats.ArgMaxProb.BoostedNoHuntProb;
import compstrategies.probstrats.ArgMaxProb.BoostedProb;
import compstrategies.standardstrats.RandomHuntStrat;
import compstrategies.standardstrats.RandomStrat;

import java.io.IOException;
import java.util.ArrayList;

public class Game{
   private int maxTurns, turnsPlayed, dimension, nCells;
   private char[] hitMap;
   private Ocean playerOcean;
   private int xShot, yShot;
   private boolean gameWon = false;
   private ArrayList<Ship> boats;
   private int[] sunkBoatIDX = new int[]{1,1,1,1,1};


   public Game(int mapSize, int maxNumTurns){
       try{playerOcean = new Ocean(mapSize);}
       catch(Exception e){System.out.println(e);}
       turnsPlayed = 0;
       dimension = mapSize;
        nCells = mapSize*mapSize;

       hitMap = new char[nCells];
       //grid = playerOcean.getGrid();
       for(int i = 0; i < nCells; i++) {
           hitMap[i] = '0';
       }
       if(maxNumTurns > mapSize * 10){throw new IllegalArgumentException("Max number of turns for the game must be less than 10x the dimension of the map");}
       maxTurns = maxNumTurns;
       playerOcean.placeRandomBoats();
       boats = playerOcean.getBoats();
   }
   private void shoot(int pos) throws IllegalArgumentException{
       if(pos < 0 || pos >= nCells){ 
           throw new IllegalArgumentException("X-coordinate is out of bounds");
       }
       if(hitMap[pos] != '0'){
           throw new IllegalArgumentException("Spot has already been shot at");
       }
       if(playerOcean.getGrid()[pos] != 'e'){
           hitMap[pos] = 'X';
           for(Ship s: boats){
               for(int i = 0; i < s.getSpots().length; i++){
                   if(s.getSpots()[i] == pos){
                       s.getSpots()[i] = -1;
                   }
               }
           }
       } else{hitMap[pos] = 'O';}
   }
   public void playGameOnTerminal_Human(){
       Scanner shotInput = new Scanner(System.in);


       for(int i = 0; i < maxTurns; i++){
           //print out the board
           for(int j = 0; j < playerOcean.getDimension(); j++){
               for(int k = 0; k < playerOcean.getDimension(); k++){
                   System.out.print(hitMap[j*dimension+k] + " ");
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
                   shoot(xShot*dimension + yShot);
                   isShotFired = true;
               }catch(IllegalArgumentException e){System.out.println(e);}
           }while(!isShotFired);
          
       }
       if(!gameWon){System.out.println("You Lose :(");}
       shotInput.close();
   }
    public void playGameComputer() throws IOException{
        //RandomStrat r = new RandomStrat(dimension);
        //ProbabilityMapStrat r = new ProbabilityMapStrat(dimension);
        //recalculateProbMap r = new recalculateProbMap(dimension);
        //RandomHuntStrat r = new RandomHuntStrat(dimension);
        //Parity r = new Parity(dimension);
        //FinalProbMap r = new FinalProbMap(dimension);
        BoostedNoHuntProb r = new BoostedNoHuntProb(dimension);
        for(int i = 0; i < dimension*dimension + 1; i++){
            if(playerOcean.isAllSunk()){gameWon = true;break;}
            turnsPlayed += 1;
            int shot = r.selectShot();
            shoot(shot);
            int sunkBoatLength = 0;
            int[] sunkCells = null;
            for (int s = 0; s < boats.size(); s++) {
                if (boats.get(s).isSunk() && sunkBoatIDX[s] == 1) {
                    sunkBoatLength = boats.get(s).getBoatLength();
                    sunkBoatIDX[s] = 0;

                    // immutable 1-D indices for this boat
                    int[] saved = boats.get(s).getSaveSpots();

                    // keep only valid cell ids
                    if (saved != null) {
                        int cnt = 0;
                        for (int id : saved) if (id >= 0 && id < nCells) cnt++;

                        if (cnt > 0) {
                            sunkCells = new int[cnt];
                            int idx = 0;
                            for (int id : saved) {
                                if (id >= 0 && id < nCells) sunkCells[idx++] = id;
                            }
                        } else {
                            sunkCells = null; // nothing valid (shouldn't happen)
                        }
                    }
                    break; // only one new sink per shot
                }
            }
            r.trackShot(playerOcean.isHit(shot), sunkBoatLength, shot, sunkCells);
            /* 
            r.printProb();
            System.out.println("\n\n");
            r.printShots();
            System.out.println("\n\n");
            */
        }

        if(!gameWon){throw new IOException("Strategy failed to complete the game");}
        //due to shoot mechanic that prevents refires on the same square, this should never happen
    }
    public int getTurnsPlayed(){
        return turnsPlayed;
    }
}
