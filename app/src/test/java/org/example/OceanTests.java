package app.src.test.java.org.example;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import app.src.main.Game.Ocean;
import java.util.ArrayList;

class OceanTest {
    @Nested
    @DisplayName("Constructor: size validation & initialization")
    class CtorTests {
        @Test
        @DisplayName("Throws when n <= 0")
        void ctorThrowsWhenNonPositive() {
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> new Ocean(0));
            assertTrue(ex.getMessage().toLowerCase().contains("greater than zero"));
        }

        @Test
        @DisplayName("Throws when n > 20")
        void ctorThrowsWhenTooLarge() {
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> new Ocean(21));
            assertTrue(ex.getMessage().toLowerCase().contains("less than or equal"));
        }

        @Test
        @DisplayName("Initializes an n x n grid of zeros")
        void ctorInitializesZeroGrid() throws Exception {
            Ocean o = new Ocean(10);
            int[][] grid = o.getGrid();
            assertEquals(10, grid.length, "rows");
            for (int r = 0; r < 10; r++) {
                assertEquals(10, grid[r].length, "cols at row " + r);
                for (int c = 0; c < 10; c++) {
                    assertEquals(0, grid[r][c], "grid[" + r + "][" + c + "] should start at 0");
                }
            }
        }
    }

    @Nested
    @DisplayName("addBoats: current behavior (using params as bounds, grid[x][y])")
    class AddBoatsTests {

        @Test
        @DisplayName("Places a vertical 'destroyer' (size ~3) at (x=1,y=1) when bounds params are large enough")
        void addBoatsPlacesWhenParamsPermit() throws Exception {
            Ocean o = new Ocean(10);

            // As written, addBoats(rowParam, colParam, orientation, size):
            // - Constructs Ship at (rowParam, colParam) (treated as x,y)
            // - Bounds check compares ship coords to *those same method params* instead of board size
            // - Writes to grid[boatSpotx][boatSpoty] (i.e., grid[x][y])
            //
            // To allow placement, pass large "row/col" params (10, 10) and start near origin.
            o.addBoats(1, 1, true, "destroyer"); // vertical: x=1; y=1..3

            int[][] g = o.getGrid();
            // Because code writes grid[x][y], expect:
            assertEquals(1, g[1][1]); // (x=1,y=1) => grid[1][1]
            assertEquals(1, g[1][2]); // (x=1,y=2) => grid[1][2]
            assertEquals(1, g[1][3]); // (x=1,y=3) => grid[1][3]
        }

        @Test
        @DisplayName("Overlap triggers IllegalArgumentException")
        void overlapThrows() throws Exception {
            Ocean o = new Ocean(10);

            o.addBoats(1, 1, true, "destroyer"); // occupies (1,1),(1,2),(1,3) in grid[x][y]
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> o.addBoats(1, 2, true, "submarine")); // would overlap at (1,2)
            assertTrue(ex.getMessage().toLowerCase().contains("overlaps"));
        }

        @Test
        @DisplayName("Out-of-bounds check uses the passed (row,col) params, not board size (demonstrates bug)")
        void paramsUsedAsBoundsNotBoardSize() throws Exception {
            Ocean o = new Ocean(10);

            // Here we pass tiny bounds (1,1). Even though the board is 10x10,
            // the method compares boat coords against 1 and will throw.
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> o.addBoats(1, 1, true, "destroyer"));
            assertTrue(ex.getMessage().toLowerCase().contains("out of bounds"));
        }
    }

    @Nested
    @DisplayName("PlaceBoats: currently iterates 5 times, catches & prints errors")
    class PlaceBoatsTests {
        @Test
        @DisplayName("No boats preloaded: PlaceBoats() prints errors and leaves grid unchanged")
        void placeBoatsNoBoats() throws Exception {
            Ocean o = new Ocean(5);
            int[][] before = deepCopy(o.getGrid());

            // With no entries in Boats list, Boats.get(i) throws; method catches and prints.
            o.PlaceBoats(1, 1, true);

            int[][] after = o.getGrid();
            assertGridEquals(before, after);
        }

        @Test
        @DisplayName("Preload one boat name: PlaceBoats() attempts placements; later indices error are caught")
        void placeBoatsWithOneName() throws Exception {
            Ocean o = new Ocean(10);
            ArrayList<String> boats = o.getBoats();
            boats.add("destroyer"); // only index 0 is valid; indices 1..4 will be caught as errors

            o.PlaceBoats(1, 1, true); // attempts to add destroyer at (1,1), then 4 failing gets

            int[][] g = o.getGrid();
            // Expect the first one placed at (1,1),(1,2),(1,3) under current grid[x][y] behavior
            assertEquals(1, g[1][1]);
            assertEquals(1, g[1][2]);
            assertEquals(1, g[1][3]);
        }
    }

    // --- helpers ---

    private static int[][] deepCopy(int[][] src) {
        int[][] dst = new int[src.length][];
        for (int r = 0; r < src.length; r++) {
            dst[r] = new int[src[r].length];
            System.arraycopy(src[r], 0, dst[r], 0, src[r].length);
        }
        return dst;
    }

    private static void assertGridEquals(int[][] a, int[][] b) {
        assertEquals(a.length, b.length, "row count mismatch");
        for (int r = 0; r < a.length; r++) {
            assertEquals(a[r].length, b[r].length, "col count mismatch at row " + r);
            for (int c = 0; c < a[r].length; c++) {
                assertEquals(a[r][c], b[r][c], "grid cell mismatch at [" + r + "][" + c + "]");
            }
        }
    }
}
