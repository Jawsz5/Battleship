package app.src.test.java.org.example;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.src.main.GameFiles.Ocean;

import static org.junit.jupiter.api.Assertions.*;

class OceanTests{
    // Constructor

    @Test
    @DisplayName("Constructor rejects dim <= 0 and > 20")
    void ctorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Ocean(0));
        assertThrows(IllegalArgumentException.class, () -> new Ocean(-1));
        assertThrows(IllegalArgumentException.class, () -> new Ocean(21));
    }

    @Test
    @DisplayName("Constructor initializes an n×n grid of 'e'")
    void ctorInitializesGrid() throws Exception {
        Ocean o = new Ocean(10);
        assertEquals(10, o.getDimension());
        char[][] g = o.getGrid();
        assertEquals(10, g.length, "row count");
        for (int r = 0; r < g.length; r++) {
            assertEquals(10, g[r].length, "col count at row " + r);
            for (int c = 0; c < g[r].length; c++) {
                assertEquals('e', g[r][c], "grid[" + r + "][" + c + "] should be 'e'");
            }
        }
    }

    // addBoats 

    @Test
    @DisplayName("addBoats: vertical 'destroyer' at (1,1) occupies (1,1),(1,2),(1,3)")
    void addBoatsVerticalSuccess() throws Exception {
        Ocean o = new Ocean(10);
        o.addBoats(1, 1, true, "destroyer"); // size 3, vertical → x constant, y increasing

        char[][] g = o.getGrid();
        assertEquals('d', g[1][1]);
        assertEquals('d', g[1][2]);
        assertEquals('d', g[1][3]);
        assertEquals(1, o.getBoats().size(), "One boat should be stored");
    }

    @Test
    @DisplayName("addBoats: horizontal then overlapping placement throws")
    void addBoatsOverlapThrows() throws Exception {
        Ocean o = new Ocean(10);
        o.addBoats(1, 1, false, "destroyer"); // occupies (1,1),(2,1),(3,1)

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> o.addBoats(2, 1, false, "submarine")); // would overlap at (2,1)
        assertTrue(ex.getMessage().toLowerCase().contains("overlap"));
    }

    @Test
    @DisplayName("addBoats: out-of-bounds throws (last index >= dimension)")
    void addBoatsOutOfBoundsThrows() throws Exception {
        Ocean o = new Ocean(5);
        // Horizontal destroyer from x=3 → cells (3,0),(4,0),(5,0); last X==5 triggers >= dimension
        assertThrows(IllegalArgumentException.class,
                () -> o.addBoats(3, 0, false, "destroyer"));

        // Vertical destroyer from y=3 → cells (0,3),(0,4),(0,5); last Y==5 triggers >= dimension
        assertThrows(IllegalArgumentException.class,
                () -> o.addBoats(0, 3, true, "destroyer"));
    }

    // placeBoats (fixed coordinates, catches errors internally)

    @Test
    @DisplayName("placeBoats: tries five names at same spot; only first should succeed")
    void placeBoatsSameSpot() throws Exception {
        Ocean o = new Ocean(10);
        o.placeBoats(0, 0, true); // 'aircraftcarrier' first, others overlap and are caught

        assertEquals(1, o.getBoats().size(), "Only the first placement should succeed");
        char[][] g = o.getGrid();
        assertEquals('a', g[0][0], "First cell should belong to the aircraft carrier");
        assertEquals('a', g[0][4], "Fifth cell of vertical aircraft carrier should be marked");
    }

    // placeRandomBoats (randomized; should place all five types)

    @Test
    @DisplayName("placeRandomBoats: places all five boats; total filled cells == 17")
    void placeRandomBoatsPlacesFive() throws Exception {
        Ocean o = new Ocean(10);
        o.placeRandomBoats();

        assertEquals(5, o.getBoats().size(), "Five boats should be stored");
        char[][] g = o.getGrid();
        int filled = 0;
        for (int r = 0; r < g.length; r++) {
            for (int c = 0; c < g[r].length; c++) {
                if (g[r][c] != 'e') filled++;
            }
        }
        // Sizes: 5 + 4 + 3 + 3 + 2 = 17 (no overlaps allowed)
        assertEquals(17, filled, "Total occupied cells should equal sum of boat sizes");
    }

    // isHit

    @Test
    @DisplayName("isHit: true on boat cell, false on empty")
    void isHitWorks() throws Exception {
        Ocean o = new Ocean(10);
        o.addBoats(3, 4, false, "submarine"); // horizontal size 2 → (3,4),(4,4)

        assertTrue(o.isHit(3, 4), "Expected hit at (3,4)");
        assertTrue(o.isHit(4, 4), "Expected hit at (4,4)");
        assertFalse(o.isHit(2, 4), "Expected miss at (2,4)");
        assertFalse(o.isHit(3, 5), "Expected miss at (3,5)");
    }
}
