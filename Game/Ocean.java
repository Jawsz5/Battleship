package Game;
import java.util.ArrayList;

public class Ocean{
    protected int row;
    protected int col;
    protected int[][] grid;
    private ArrayList<String> Boats = new ArrayList<>();

    public Ocean(int row, int col)throws Exception {
        if(row <= 0 || col <= 0) {
            throw new Exception("Row and column must be greater than zero.");
        }
        else if(row > 20 || col > 20){
            throw new Exception("Row and column must be less than or equal to 20.");
        }
        else{
            this.row = row;
            this.col = col;
            grid = new int[row][col];
            for(int i = 0; i < row; i++) {
                for(int j = 0; j < col; j++) {
                    grid[i][j] = 0; // Initialize the grid with zeros
                }
            }

        }
    }
    public addBoats(int row, int col, Boolean orientation, String size)throws Exception{
        createBoat(row, col, orientation, size);
        int[] boatSpotx = getSpotsX();
        int[] boatSpoty = getSpotsY();
        for(int i = 0 i < boatSpotx.length; i++){
            if(boatSpotx[i] < row && boatSpoty[i] < col && boatSpotx[i] >= 0 && boatSpoty[i] >= 0){
                if(grid[boatSpotx[i]][boatSpoty[i]] == 0){
                    grid[boatSpotx[i]][boatSpoty[i]] = 1; // Mark the boat's position
                }else {
                    throw new Exception("Boat overlaps with another boat or is out of bounds.");
                }

            }else{
                throw new Exception("Boat is out of bounds.");
            }
        
    }
    Boats.add(size);
    
}
                
    public void PlaceBoats(int row, int col, Boolean orientation){
        for(int i = 0; i < 5; i++){
            try {
                addBoats(row, col, orientation, Boats.get(i));
            } catch (Exception e) {
                System.out.println("Error placing boat");
            }

        }
    }
}