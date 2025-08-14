package app.src.test.java.org.example;
import org.junit.jupiter.api.Test;

import app.src.main.GameFiles.Ship;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

public class ShipTests {
    @Test
    @DisplayName("Boolean+String ctor: vertical placement builds spots along +Y")
    void booleanCtorVerticalSpots() {
        Ship s = new Ship(2, 3, true, "destroyer"); // size 3
        assertArrayEquals(new int[]{2, 2, 2}, s.getSpotsX(), "X should be constant when vertical");
        assertArrayEquals(new int[]{3, 4, 5}, s.getSpotsY(), "Y should increment from start");
        assertEquals(2, s.getX());
        assertEquals(3, s.getY());
        assertEquals('d', s.getBoatType());
    }

    @Test
    @DisplayName("Boolean+String ctor: horizontal placement builds spots along +X")
    void booleanCtorHorizontalSpots() {
        Ship s = new Ship(5, 1, false, "cruiser"); // size 3
        assertArrayEquals(new int[]{5, 6, 7}, s.getSpotsX(), "X should increment when horizontal");
        assertArrayEquals(new int[]{1, 1, 1}, s.getSpotsY(), "Y should be constant");
        assertEquals(5, s.getX());
        assertEquals(1, s.getY());
        assertEquals('c', s.getBoatType());
    }

    @Test
    @DisplayName("String+String ctor: 'vertical' produces vertical spots; 'submarine' size=2")
    void stringCtorVerticalSpots() {
        Ship s = new Ship(0, 0, "vertical", "submarine"); // size 2
        assertArrayEquals(new int[]{0, 0}, s.getSpotsX());
        assertArrayEquals(new int[]{0, 1}, s.getSpotsY());
        assertEquals('s', s.getBoatType());
    }

    @Test
    @DisplayName("String direction: unknown string defaults to horizontal (no exception)")
    void stringDirectionUnknownDefaultsHorizontal() {
        Ship s = new Ship(1, 1, "diagonal?", "destroyer"); // unknown => isVertical=false
        assertArrayEquals(new int[]{1, 2, 3}, s.getSpotsX());
        assertArrayEquals(new int[]{1, 1, 1}, s.getSpotsY());
    }

    @Test
    @DisplayName("Size aliases map correctly (aircraftcarrier/air/a -> size 5, type 'a')")
    void sizeAliasMapping() {
        Ship s1 = new Ship(0, 0, true, "air");
        assertEquals('a', s1.getBoatType());
        assertEquals(5, s1.getSpotsX().length);

        Ship s2 = new Ship(0, 0, true, "aircraftcarrier");
        assertEquals('a', s2.getBoatType());
        assertEquals(5, s2.getSpotsX().length);
    }

    @Test
    @DisplayName("Invalid size strings are swallowed: arrays end up zero-length")
    void invalidSizeCreatesZeroLengthArrays() {
        Ship s = new Ship(0, 0, true, "aircraft carrier"); // has a space; not matched by current parser
        assertEquals(0, s.getSpotsX().length, "spotsX length is 0 because size parsing failed but was swallowed");
        assertEquals(0, s.getSpotsY().length, "spotsY length is 0 because size parsing failed but was swallowed");
    }

    @Test
    @DisplayName("Null Boolean direction triggers NPE during populateSpots (isVertical auto-unboxing)")
    void nullBooleanDirectionCausesNpe() {
        assertThrows(NullPointerException.class, () -> new Ship(0, 0, (Boolean) null, "destroyer"));
    }

    @Test
    @DisplayName("Null String direction throws IllegalArgumentException from setDirection(String)")
    void nullStringDirectionThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new Ship(0, 0, (String) null, "destroyer"));
    }

    @Test
    @DisplayName("isHit()/hit(): horizontal ship marks X at the match index, Y at the last matching index (current behavior)")
    void hitMarksIndependentIndicesHorizontal() {
        // Horizontal: (2,3),(3,3),(4,3)
        Ship s = new Ship(2, 3, false, "destroyer"); // size 3
        Boolean wasHit = s.isHit(3, 3);
        assertEquals(Boolean.TRUE, wasHit, "Expected hit to return true");

        // Current code: shotX is index of matching X (1), shotY is last matching Y (2)
        assertArrayEquals(new int[]{2, -1, 4}, s.getSpotsX(), "Only X at index 1 is set to -1");
        assertArrayEquals(new int[]{3, 3, -1}, s.getSpotsY(), "Y at last match index (2) is set to -1");
    }

    @Test
    @DisplayName("isHit(): miss returns false and arrays unchanged")
    void missReturnsFalseAndNoChange() {
        Ship s = new Ship(10, 10, true, "cruiser"); // vertical: (10,10),(10,11),(10,12)
        int[] beforeX = s.getSpotsX().clone();
        int[] beforeY = s.getSpotsY().clone();

        Boolean wasHit = s.isHit(99, 99);
        assertEquals(Boolean.FALSE, wasHit, "Miss should return false");
        assertArrayEquals(beforeX, s.getSpotsX(), "X unchanged on miss");
        assertArrayEquals(beforeY, s.getSpotsY(), "Y unchanged on miss");
    }
}
