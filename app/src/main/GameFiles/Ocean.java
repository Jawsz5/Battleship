package app.src.main.GameFiles;
import java.util.ArrayList;

public class Ocean{
    private int dimension;
    private char[][] grid;
    private ArrayList<Ship> Boats = new ArrayList<Ship>();
    private String[] boatNames = new String[]{"aircraft carrier", "battleship", "destroyer", "cruiser", "submarine"};

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
        if(x < 0 || y < 0 || boatSpotX[boatLength - 1] > dimension || boatSpotY[boatLength - 1] > dimension){ //check if boat is out of bounds
            throw new IllegalArgumentException("Boat is out of bounds.");
        }
        for(int i = 0; i < boatLength; i++){ //check if boat overlaps with another boat
            if(grid[boatSpotX[i]][boatSpotY[i]] == 0){
                grid[boatSpotX[i]][boatSpotY[i]] = s.getBoatType(); //using boatType instead of 1 in order to make the ocean more descriptive
            } else{
                throw new IllegalArgumentException("Boat overlaps with another boat");
            }
        }
        Boats.add(s);
    }
    //MOURYA this isn't manual or random. wtf does this do
    public void placeBoats(int row, int col, Boolean orientation){
        for(int i = 0; i < 5; i++){
            try {
                addBoats(row, col, orientation, boatNames[i]);
            } catch (Exception e) {
                System.out.println("Error placing boat");
            }

        }
    }
    public void placeRandomBoats(){
        int boatsPlaced = 0;
        do{
            int x = (int) Math.random() * dimension;
            int y = (int) Math.random() * dimension;
            Boolean direction = Math.random() < 0.5;
            String size = boatNames[boatsPlaced];
            try{
                addBoats(x, y, direction, size);
            } catch(Exception e){}
            boatsPlaced += 1;
        }while(boatsPlaced != 5);
    }
    public Boolean isHit(int x, int y){
        //checks to see if a boat is on a sqaure
        if(grid[x][y] != 0){
            return true;
        }
        return false;
    }

    public char[][] getGrid(){return grid;}
    public ArrayList<Ship> getBoats(){return Boats;}
    public int getDimension(){return dimension;}
}