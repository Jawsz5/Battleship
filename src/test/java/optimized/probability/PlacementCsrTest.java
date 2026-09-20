package optimized.probability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlacementCsrTest {
    @Test
    void graphHasExactRequiredDimensions() {
        assertEquals(600, PlacementCsr.PLACEMENT_COUNT);
        assertEquals(2_000, PlacementCsr.INCIDENCE_COUNT);
        assertEquals(601, PlacementCsr.placementOffsetSize());
        assertEquals(2_000, PlacementCsr.placementCellsSize());
        assertEquals(600, PlacementCsr.placementLengthSize());
        assertEquals(101, PlacementCsr.cellOffsetSize());
        assertEquals(2_000, PlacementCsr.cellPlacementsSize());
        assertEquals(10, PlacementCsr.INITIAL_ACTIVE.length);
    }

    @Test
    void bothCsrDirectionsContainTheSameIncidences() {
        boolean[][] placementToCell = new boolean[600][100];
        int incidences = 0;
        for (int placement = 0; placement < 600; placement++) {
            int length = PlacementCsr.PLACEMENT_LENGTH[placement];
            int from = PlacementCsr.PLACEMENT_CELL_OFFSET[placement];
            int to = PlacementCsr.PLACEMENT_CELL_OFFSET[placement + 1];
            assertEquals(length, to - from);
            for (int at = from; at < to; at++) {
                int cell = PlacementCsr.PLACEMENT_CELLS[at];
                assertTrue(cell >= 0 && cell < 100);
                placementToCell[placement][cell] = true;
                incidences++;
            }
        }
        assertEquals(2_000, incidences);

        int reverseIncidences = 0;
        for (int cell = 0; cell < 100; cell++) {
            for (int at = PlacementCsr.CELL_PLACEMENT_OFFSET[cell];
                    at < PlacementCsr.CELL_PLACEMENT_OFFSET[cell + 1]; at++) {
                int placement = PlacementCsr.CELL_PLACEMENTS[at];
                assertTrue(placementToCell[placement][cell]);
                reverseIncidences++;
            }
        }
        assertEquals(2_000, reverseIncidences);
    }
}
