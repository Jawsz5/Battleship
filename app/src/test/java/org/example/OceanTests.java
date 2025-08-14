package app.src.test.java.org.example;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.src.main.Game.Ocean;

import static org.junit.jupiter.api.Assertions.*;

class OceanTests{

    @Test
    @DisplayName("Constructor rejects sizes <= 0")
    void ctorRejectsNonPositive() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Ocean(0));
        assertTrue(ex.getMessage().toLowerCase().contains("greater than zero"));
        assertThrows(IllegalArgumentException.class, () -> new Ocean(-5));
    }

    @Test
    @DisplayName("Constructor rejects sizes > 20")
    void ctorRejectsTooLarge() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Ocean(21));
        assertTrue(ex.getMessage().toLowerCase().contains("less than or equal"));
        assertThrows(IllegalArgumentException.class, () -> new Ocean(100));
    }

    @Test
    @DisplayName("Constructor initializes an n×n grid of zeros")
    void ctorInitializesZeroGrid() throws Exception {
        Ocean o = new Ocean(10);
        int[][] grid = o.getGrid();
        assertEquals(10, grid.length, "row count");
        for (int r = 0; r < grid.length; r++) {
            assertEquals(10, grid[r].length, "col count at row " + r);
            for (int c = 0; c < grid[r].length; c++) {
                assertEquals(0, grid[r][c], "grid[" + r + "][" + c + "] should be 0");
            }
        }
    }

    @Test
    @DisplayName("addBoats uses its parameters as bounds (demonstrates out-of-bounds throw for small values)")
    void addBoatsParamsAsBoundsCausesOob() throws Exception {
        Ocean o = new Ocean(10);
        // As implemented, addBoats(row,col,...) compares boat spots to those same params (< row, < col).
        // With small params like (1,1), any multi-cell ship will throw "Boat is out of bounds."
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> o.addBoats(10, 10, true, "destroyer"));
        assertTrue(ex.getMessage().toLowerCase().contains("out of bounds"));
    }

    @Test
    @DisplayName("addBoats throws overlap when a cell is already marked (if first placement succeeded upstream)")
    void addBoatsOverlapThrowsWhenCellOccupied() throws Exception {
        // This test asserts the overlap branch by pre-marking a cell directly on the grid,
        // since addBoats itself cannot successfully place with small bounds parameters.
        Ocean o = new Ocean(10);
        int[][] grid = o.getGrid();

        // Pre-mark a cell that a vertical ship starting at (0,0) would try to use.
        // We'll choose to simulate an overlap at (0,0).
        grid[0][0] = 1;

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> o.addBoats(0, 0, true, "submarine"));
        assertTrue(ex.getMessage().toLowerCase().contains("overlaps"));
    }

    @Test
    @DisplayName("PlaceBoats with empty Boats list: catches errors and leaves grid unchanged")
    void placeBoatsEmptyListNoChange() throws Exception {
        Ocean o = new Ocean(5);
        int[][] before = o.getGrid();

        // Call PlaceBoats; with an empty Boats list, Boats.get(i) throws inside the loop,
        // which is caught and only logs "Error placing boat".
        o.placeBoats(1, 1, true);

        int[][] after = o.getGrid();
        assertSame(before, after, "Grid instance should be the same");
        for (int r = 0; r < after.length; r++) {
            for (int c = 0; c < after[r].length; c++) {
                assertEquals(0, after[r][c], "grid cell should remain 0 at [" + r + "][" + c + "]");
            }
        }
    }

    @Test
    @DisplayName("getBoats exposes the internal list (mutations via getter are reflected)")
    void getBoatsIsLiveList() throws Exception {
        Ocean o = new Ocean(10);
        assertEquals(0, o.getBoats().size());
        o.getBoats().add("destroyer");
        assertEquals(1, o.getBoats().size(), "Adding through getter should reflect on subsequent getBoats()");
    }
}
