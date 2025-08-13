package app.src.test.java.org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.src.main.Game.Ocean;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

class OceanTest {

    Ocean ocean;

    @BeforeEach
    void setUp() throws Exception {
        ocean = new Ocean(10);
    }

    // 1. Reject size 0
    @Test
    void testConstructorRejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new Ocean(0));
    }

    // 2. Reject negative size
    @Test
    void testConstructorRejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Ocean(-1));
    }

    // 3. Reject size > 20
    @Test
    void testConstructorRejectsTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> new Ocean(25));
    }

    // 4. Correct grid dimensions
    @Test
    void testGridDimensions() throws Exception {
        Ocean o = new Ocean(8);
        assertEquals(8, o.grid.length);
        assertEquals(8, o.grid[0].length);
    }

    // 5. Grid initialized to zeros
    @Test
    void testGridInitializedToZero() throws Exception {
        Ocean o = new Ocean(5);
        for (int[] row : o.grid) {
            for (int cell : row) {
                assertEquals(0, cell);
            }
        }
    }

    // 6. Place boat without overlaps
    @Test
    void testAddBoatsValidPlacement() throws Exception {
        // Use int size so Ship constructor initializes properly
        Ship s = new Ship(2, 2, true, 3);
        int[] xs = s.getSpotsX();
        int[] ys = s.getSpotsY();
        for (int i = 0; i < xs.length; i++) {
            ocean.grid[xs[i]][ys[i]] = 0;
        }
        ocean.addBoats(2, 2, true, "destroyer"); // relies on Ship string size
        for (int i = 0; i < xs.length; i++) {
            assertEquals(1, ocean.grid[xs[i]][ys[i]]);
        }
    }

    // 7. Throw if boat overlaps
    @Test
    void testAddBoatsOverlapThrows() throws Exception {
        // Fill one cell
        ocean.grid[1][1] = 1;
        assertThrows(IllegalArgumentException.class, () ->
                ocean.addBoats(1, 1, true, "submarine"));
    }

    // 8. Throw if out of bounds
    @Test
    void testAddBoatsOutOfBoundsThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                ocean.addBoats(15, 15, true, "cruiser"));
    }

    // 9. Boats list updated
    @Test
    void testBoatsListUpdated() throws Exception {
        ocean.addBoats(1, 1, true, "cruiser");
        assertTrue(ocean.Boats.contains("cruiser"));
    }

    // 10. PlaceBoats catches and reports errors
    @Test
    void testPlaceBoatsErrorHandling() {
        ocean.Boats.addAll(Arrays.asList("cruiser", "destroyer", "submarine", "battleship", "aircraft"));
        assertDoesNotThrow(() -> ocean.PlaceBoats(1, 1, true));
    }
}
