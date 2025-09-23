import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import gamefiles.Ship;

public class ShipTest {

    @Test
    @DisplayName("Boolean ctor: vertical builds 1-D spots pos, pos+10, ... (len=3)")
    void booleanCtorVerticalSpots() {
        Ship s = new Ship(23, true, "destroyer"); // size 3
        assertArrayEquals(new int[]{23, 33, 43}, s.getSpots(),
                "Vertical ship should step by +10 on a 10x10 board");
        assertEquals('d', s.getBoatType());
    }

    @Test
    @DisplayName("Boolean ctor: horizontal builds 1-D spots pos, pos+1, ... (len=3)")
    void booleanCtorHorizontalSpots() {
        Ship s = new Ship(5, false, "cruiser"); // size 3
        assertArrayEquals(new int[]{5, 6, 7}, s.getSpots(),
                "Horizontal ship should step by +1");
        assertEquals('c', s.getBoatType());
    }

    @Test
    @DisplayName("String ctor: 'vertical' (and aliases) produces vertical 1-D spots")
    void stringCtorVerticalSpots() {
        Ship s = new Ship(0, "vertical", "submarine"); // size 2
        assertArrayEquals(new int[]{0, 10}, s.getSpots());
        assertEquals('s', s.getBoatType());
    }

    @Test
    @DisplayName("String direction: unknown string defaults to horizontal (no exception)")
    void stringDirectionUnknownDefaultsHorizontal() {
        Ship s = new Ship(1, "diagonal?", "destroyer"); // unknown => horizontal
        assertArrayEquals(new int[]{1, 2, 3}, s.getSpots());
    }

    @Test
    @DisplayName("Size aliases map correctly (aircraftcarrier/air/a -> size 5, type 'a')")
    void sizeAliasMapping() {
        Ship s1 = new Ship(0, true, "air");
        assertEquals('a', s1.getBoatType());
        assertEquals(5, s1.getSpots().length);

        Ship s2 = new Ship(0, true, "aircraftcarrier");
        assertEquals('a', s2.getBoatType());
        assertEquals(5, s2.getSpots().length);
    }

    @Test
    @DisplayName("Invalid size strings are swallowed: spots ends up zero-length")
    void invalidSizeCreatesZeroLengthArrays() {
        Ship s = new Ship(0, true, "aircraft carrier"); // space not matched by parser
        assertEquals(0, s.getSpots().length, "spots length is 0 because size parsing failed but was swallowed");
    }

    @Test
    @DisplayName("Null Boolean direction throws NullPointerException")
    void nullBooleanDirectionThrowsIAE() {
        assertThrows(NullPointerException.class, () -> new Ship(0, (Boolean) null, "destroyer"));
    }

    @Test
    @DisplayName("Null String direction throws IllegalArgumentException")
    void nullStringDirectionThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new Ship(0, (String) null, "destroyer"));
    }

    @Test
    @DisplayName("isHit()/hit(): horizontal ship marks only the matching 1-D index as -1")
    void hitMarksSingleIndexHorizontal() {
        Ship s = new Ship(2, false, "destroyer"); // {2,3,4}
        Boolean wasHit = s.isHit(3);
        assertEquals(Boolean.TRUE, wasHit, "Expected hit to return true");
        assertArrayEquals(new int[]{2, -1, 4}, s.getSpots(), "Only the hit index should be set to -1");
    }

    @Test
    @DisplayName("isHit(): miss returns false and array unchanged")
    void missReturnsFalseAndNoChange() {
        Ship s = new Ship(34, true, "cruiser"); // {34,44,54}
        int[] before = s.getSpots().clone();

        Boolean wasHit = s.isHit(35);
        assertEquals(Boolean.FALSE, wasHit, "Miss should return false");
        assertArrayEquals(before, s.getSpots(), "Spots unchanged on miss");
    }
}
