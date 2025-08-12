package Game;

public class Ocean{
    protected int row;
    protected int col;
    protected int[][] grid;

    public Ocena(int row, int col)throws Exception {
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
}