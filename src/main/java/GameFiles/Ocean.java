package gamefiles;

import java.util.ArrayList;
import java.util.Scanner;


public class Ocean{
   private int dimension, nCells;
   private char[] grid;
   private ArrayList<Ship> Boats = new ArrayList<Ship>();
   private String[] boatNames = new String[]{"aircraftcarrier", "battleship", "destroyer", "cruiser", "submarine"};


   public Ocean(int dim)throws Exception {
       dimension = dim;
       nCells = dimension*dimension;
       if(dimension <= 0 || dimension > 20){ //arbitrary max map size of 20x20
           throw new IllegalArgumentException("Row and column must be greater than zero and less than or equal to 20");
       }
       else{
           grid = new char[nCells];
           for(int i = 0; i < nCells; i++) {   
                grid[i] = 'e'; // Initialize the grid with zeros
           }
       }
   }
   public void addBoats(int x, int y, Boolean direction, String size) throws Exception{
       Ship s = new Ship(x, y, direction, size);
       int[] boatSpotX = s.getSpotsX();
       int[] boatSpotY = s.getSpotsY();
       int boatLength = boatSpotX.length;
       if(x < 0 || y < 0 || boatSpotX[boatLength - 1] >= dimension || boatSpotY[boatLength - 1] >= dimension){ //check if boat is out of bounds
           throw new IllegalArgumentException("Boat is out of bounds.");
       }
       for(int i = 0; i < boatLength; i++){ //check if boat overlaps with another boat
           if(grid[10*boatSpotX[i] + boatSpotY[i]] != 'e'){
               throw new IllegalArgumentException("Boat overlaps with another boat");
           }
       }
       for(int i = 0; i < boatLength; i ++){
           grid[10*boatSpotX[i] + boatSpotY[i]] = s.getBoatType();
       }
       Boats.add(s);
   }
   
   public void placeBoatsManual(){
    int boatsPlaced = 0;
    try (Scanner scan = new Scanner(System.in)) {
        do{
            System.out.println("Enter a starting row: ");
            int startRow = scan.nextInt();
            System.out.println("Enter a starting Column: ");
            int startCol = scan.nextInt();
            System.out.println("Enter a true or false for boat direction (True for Vertical; False for Horizontal): ");
            boolean orientation = scan.nextBoolean();
            System.out.println("Enter the name of the boat (aircraft, battleship, cruiser, destroyer, submarine)");
            scan.nextLine();
            String size = scan.nextLine();
            try{
                addBoats(startRow, startCol, orientation, size);
                System.out.println("Boat placed successfully");
                boatsPlaced += 1;


            }catch(Exception e){System.out.println("Boat Failed to place");}

        }while(boatsPlaced < 5);
    }
    
   }
   public void placeRandomBoats(){
       int boatsPlaced = 0;
       do{
           //System.out.println(boatsPlaced);
           int x = (int) (Math.random() * dimension);
           int y = (int) (Math.random() * dimension);
           Boolean direction = Math.random() < 0.5;
           String size = boatNames[boatsPlaced];
           try{
               addBoats(x, y, direction, size);
               boatsPlaced += 1;
           } catch(Exception e){}
       }while(boatsPlaced != 5);
   }

   public Boolean isHit(int x, int y){
       //checks to see if a boat is on a sqaure
       if(grid[10*x + y] != 'e'){
           return true;
       }
       return false;
   }
   public Boolean isAllSunk(){
       for(Ship ship : Boats){
           if(!ship.isSunk()){
               return false; //if any ship is not sunk, return false
           }
       }
       return true;
   }


   public char[] getGrid(){return grid;}
   public ArrayList<Ship> getBoats(){return Boats;}
   public int getDimension(){return dimension;}
}
