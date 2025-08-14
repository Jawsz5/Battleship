package app.src.main.Game;
import java.util.ArrayList;

public class Ocean{
    private int dimension;
    private int[][] grid;
    private ArrayList<String> Boats = new ArrayList<String>();
    private String[] boatNames = new String[]{"aircraft carrier", "battleship", "destroyer", "cruiser", "submarine"};

    public Ocean(int dim)throws Exception {
        dimension = dim;
        if(dimension <= 0 || dimension > 20){ //arbitrary max map size of 20x20
            throw new IllegalArgumentException("Row and column must be greater than zero and less than or equal to 20");
        }
        else{
            grid = new int[dimension][dimension];
            for(int i = 0; i < dimension; i++) {
                for(int j = 0; j < dimension; j++) {
                    grid[i][j] = 0; // Initialize the grid with zeros
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
                grid[boatSpotX[i]][boatSpotY[i]] = 1;
            } else{
                throw new IllegalArgumentException("Boat overlaps with another boat");
            }
        }
        Boats.add(size);
    }
                
    public void placeBoats(int row, int col, Boolean orientation){
        for(int i = 0; i < 5; i++){
            try {
                addBoats(row, col, orientation, Boats.get(i));
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
            Boolean direction;
            double randDir = Math.random();
            if(randDir >= 0.5){
                direction = true; //vertical
            } else{direction = false;}
            String size = boatNames[boatsPlaced];
            try{
                addBoats(x, y, direction, size);
            } catch(Exception e){}
            boatsPlaced += 1;
        }while(boatsPlaced != 5);
    }
    public int[][] getGrid(){return grid;}
    public ArrayList<String> getBoats(){return Boats;}
    public int getDimension(){return dimension;}
}