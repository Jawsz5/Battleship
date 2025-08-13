package app.src.test.java.org.example;
import org.junit.jupiter.api.Test;

import app.src.main.Game.Ship;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;

public class ShipTests {
    @Test
    @DisplayName("Boolean ctor: horizontal ship populates spots along X")
    void booleanCtorHorizontalSpots() {
        Ship s = new Ship(2, 3, false, 3);
        assertArrayEquals(new int[]{2, 3, 4}, s.getSpotsX(), "spotsX should be 2, 3, and 4");
        assertArrayEquals(new int[]{3, 3, 3}, s.getSpotsY(), "spotsY should be constant at y=3");
        assertEquals(2, s.getX());
        assertEquals(3, s.getY());
    }

    @Test
    @DisplayName("Boolean ctor: vertical ship populates spots along Y")
    void booleanCtorVerticalSpots() {
        Ship s = new Ship(5, 1, true, 4);
        assertArrayEquals(new int[]{5, 5, 5, 5}, s.getSpotsX(), "spotsX should be constant at x=5");
        assertArrayEquals(new int[]{1, 2, 3, 4}, s.getSpotsY(), "spotsY should be 1 through 4");
        assertEquals(5, s.getX());
        assertEquals(1, s.getY());
    }

    @Test
    @DisplayName("hit()/isHit(): marking uses last Y match index and X match index independently")
    void hitMarksIndependentIndices() {
        // Horizontal ship from (2,3), size 3 -> coords: (2,3), (3,3), (4,3)
        Ship s = new Ship(2, 3, false, 3);

        // Shoot (3,3): X matches index 1; Y=3 matches all indices (0,1,2) — loop leaves shotY at last match (2)
        Boolean wasHit = s.isHit(3, 3);
        assertEquals(Boolean.TRUE, wasHit, "isHit should return true on a hit");

        // After hit: spotsX[1] set to -1; spotsY[2] set to -1 (current implementation detail)
        assertArrayEquals(new int[]{2, -1, 4}, s.getSpotsX(), "Only the X index that matched should be set to -1");
        assertArrayEquals(new int[]{3, 3, -1}, s.getSpotsY(), "Y uses last matching index (2) per current code");
    }

    @Test
    @DisplayName("isHit(): miss leaves isAHit as null and arrays unchanged")
    void missLeavesStateUntouched() {
        Ship s = new Ship(0, 0, true, 2); // coords: (0,0), (0,1)

        // Fire at a clear miss
        Boolean wasHit = s.isHit(10, 10);
        assertNull(wasHit, "On a miss, isHit returns the field (null) since code never sets it to false");
        assertArrayEquals(new int[]{0, 0}, s.getSpotsX());
        assertArrayEquals(new int[]{0, 1}, s.getSpotsY());
    }

    @Test
    @DisplayName("Boolean+String ctor: size mapping works for some names; 'aircraft' throws IOException (as coded)")
    void stringSizeMappingBehavior() {
        // destroyer -> index 2 -> size_map[2] == 3
        Ship sDestroyer = new Ship(0, 0, true, "destroyer");
        // arrays remain null due to temporary new Ship(...), but size on 'this' is set before that
        // We can't read 'size' directly (no getter), so we sanity-check no exceptions were thrown.
        assertDoesNotThrow(() -> new Ship(0, 0, true, "cruiser"));

        // "aircraft" path: size set to index 0, then code throws IOException when size == 0
        // That IOException is caught in the ctor and swallowed with a println; construction still succeeds.
        assertDoesNotThrow(() -> new Ship(0, 0, true, "aircraft"),
                "Constructor catches IOException internally; object is still constructed");
    }

    @Test
    @DisplayName("hit(): multiple hits can mark multiple indices independently")
    void multipleHits() {
        Ship s = new Ship(10, 10, false, 4); // (10,10),(11,10),(12,10),(13,10)

        // First hit at (12,10): X index 2, Y last match index 3 (since all Y=10)
        s.hit(12, 10);
        assertArrayEquals(new int[]{10, 11, -1, 13}, s.getSpotsX());
        assertArrayEquals(new int[]{10, 10, 10, -1}, s.getSpotsY());

        // Second hit at (11,10): X index 1, Y last match now index 2 (since index 3 already -1)
        s.hit(11, 10);
        assertArrayEquals(new int[]{10, -1, -1, 13}, s.getSpotsX());
        assertArrayEquals(new int[]{10, 10, -1, -1}, s.getSpotsY());
    }


}
