package GameFiles;
import java.util.ArrayList;


public class Ocean{
   private int dimension;
   private char[][] grid;
   private ArrayList<Ship> Boats = new ArrayList<Ship>();
   private String[] boatNames = new String[]{"aircraftcarrier", "battleship", "destroyer", "cruiser", "submarine"};


   public Ocean(int dim)throws Exception {
       dimension = dim;
       if(dimension <= 0 || dimension > 20){ //arbitrary max map size of 20x20
           throw new IllegalArgumentException("Row and column must be greater than zero and less than or equal to 20");
       }
       else{
           grid = new char[dimension][dimension];
           for(int i = 0; i < dimension; i++) {
               for(int j = 0; j < dimension; j++) {
                   grid[i][j] = 'e'; // Initialize the grid with zeros
               }
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
           if(grid[boatSpotX[i]][boatSpotY[i]] != 'e'){
               throw new IllegalArgumentException("Boat overlaps with another boat");
           }
       }
       for(int i = 0; i < boatLength; i ++){
           grid[boatSpotX[i]][boatSpotY[i]] = s.getBoatType();
       }
       Boats.add(s);
   }
   //MOURYA this isn't manual or random. wtf does this do
   public void placeBoatsManual(int startRow, int startCol, Boolean orientation){
       for(int i = 0; i < 5; i++){
           try {
               addBoats(startRow, startCol, orientation, boatNames[i]);
           } catch (Exception e) {
               System.out.println("Error placing boat");
           }


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
       if(grid[x][y] != 'e'){
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


   public char[][] getGrid(){return grid;}
   public ArrayList<Ship> getBoats(){return Boats;}
   public int getDimension(){return dimension;}
}
