import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import gamefiles.Ocean;

class OceanTest {

    // constructor tests
    @Test
    @DisplayName("Constructor rejects dim <= 0 and > 20")
    void ctorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Ocean(0));
        assertThrows(IllegalArgumentException.class, () -> new Ocean(-1));
        assertThrows(IllegalArgumentException.class, () -> new Ocean(21));
    }

    @Test
    @DisplayName("Constructor initializes an n×n grid of 'e' in 1-D array")
    void ctorInitializesGrid() throws Exception {
        Ocean o = new Ocean(10);
        assertEquals(10, o.getDimension());
        char[] g = o.getGrid();
        assertEquals(100, g.length, "grid must be dim*dim");
        for (char ch : g) assertEquals('e', ch);
    }

    // addBoats

    @Test
    @DisplayName("addBoats: horizontal destroyer at idx=11 occupies 11,12,13")
    void addBoatsHorizontalSuccess() throws Exception {
        Ocean o = new Ocean(10);
        o.addBoats(11, /*vertical=*/false, "destroyer"); // size 3, horizontal → +1

        char[] g = o.getGrid();
        assertEquals('d', g[11]);
        assertEquals('d', g[12]);
        assertEquals('d', g[13]);
        assertEquals(1, o.getBoats().size(), "One boat should be stored");
    }

    @Test
    @DisplayName("addBoats: overlapping placement throws")
    void addBoatsOverlapThrows() throws Exception {
        Ocean o = new Ocean(10);
        o.addBoats(11, false, "destroyer"); // occupies 11,12,13 (size 3)

        // Horizontal submarine at 12 would occupy 12,13,14 → overlaps at 12 & 13
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> o.addBoats(12, false, "submarine"));
        // If you don’t guarantee a message, you can delete this next assertion.
        assertTrue(ex.getMessage().toLowerCase().contains("overlap"));
    }
    /* 
    @Test
    @DisplayName("addBoats: out-of-bounds throws (1-D indices)")
    void addBoatsOutOfBoundsThrows() throws Exception {
        Ocean o = new Ocean(5); // dim=5, indices 0..24

        // Horizontal destroyer (len=3) starting at idx=3 → cells 3,4,5 (5 is OOB)
        assertThrows(IllegalArgumentException.class,
            () -> o.addBoats(3, false, "destroyer"));

        // Vertical destroyer starting at row=3,col=0 → idx=15 → 15,20,25 (25 OOB)
        assertThrows(IllegalArgumentException.class,
            () -> o.addBoats(3, true, "destroyer"));
    }
            */

    // placeRandomBoats

    @Test
    @DisplayName("placeRandomBoats: places all five boats; total filled cells == 17")
    void placeRandomBoatsPlacesFive() throws Exception {
        Ocean o = new Ocean(10);
        o.placeRandomBoats();

        assertEquals(5, o.getBoats().size(), "Five boats should be stored");
        int filled = 0;
        for (char ch : o.getGrid()) if (ch != 'e') filled++;
        // Sizes: 5 + 4 + 3 + 3 + 2 = 17 (no overlaps allowed)
        assertEquals(17, filled, "Total occupied cells should equal sum of boat sizes");
    }

    // isHit

    @Test
    @DisplayName("isHit: true on boat cell, false on empty (1-D indices)")
    void isHitWorks() throws Exception {
        Ocean o = new Ocean(10);

        // Place a vertical 2-length boat (patrol) at idx=34 (row=3,col=4) → cells 34 and 44
        // If your 2-length boat is named differently, adjust the string accordingly.
        o.addBoats(34, true, "battleship");
        assertTrue(o.isHit(34), "Expected hit at idx 34");
        assertTrue(o.isHit(44), "Expected hit at idx 44");
        assertFalse(o.isHit(24), "Expected miss at idx 24");
        assertFalse(o.isHit(35), "Expected miss at idx 35");
    }
}
