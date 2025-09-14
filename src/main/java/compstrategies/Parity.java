package compstrategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import compstrategies.standardstrats.RandomStrat;

public class Parity extends RandomStrat {
    private int[] hitMap;
    private Hunt h;
    private List<Integer> remainingShipLengths;
    private List<Integer> huntSquares;

    public Parity(int mapDim) {
        super(mapDim);
        hitMap = new int[nCells];
        for (int i = 0; i < nCells; i++) hitMap[i] = 0;

        // Standard ship lengths
        remainingShipLengths = new ArrayList<>();
        remainingShipLengths.add(5);
        remainingShipLengths.add(4);
        remainingShipLengths.add(3);
        remainingShipLengths.add(3);
        remainingShipLengths.add(2);

        // Checkerboard squares
        huntSquares = new ArrayList<>();
        for (int x = 0; x < dim; x++) {
            for (int y = 0; y < dim; y++) {
                if ((x + y) % 2 == 0) huntSquares.add(flatten(x, y));
            }
        }
        Collections.shuffle(huntSquares);
    }

    private int flatten(int x, int y) { return dim * x + y; }

    private boolean canFitShip(int pos) {
        int row = pos / dim;
        int col = pos % dim;
        for (int len : remainingShipLengths) {
            if (len <= dim - col || len <= dim - row) return true;
        }
        return false;
    }

    public void trackShot(boolean hit, int sunkLen, int x, int y) {
        int pos = flatten(x, y);
        hitMap[pos] = hit ? 2 : 1;

        if (hit && h == null) {
            h = new Hunt(pos, dim); // enter Hunt mode
        }

        if (sunkLen != 0) {
            remainingShipLengths.remove(Integer.valueOf(sunkLen));
            if (h != null) {
                h.reset();
                h = null;
            }
        }
    }

    public int[] selectShot() {
        int shot = -1;

        // Hunt mode
        if (h != null) {
            try {
                while (true) {
                    shot = h.huntShip(hitMap);
                    if (shot >= 0 && shot < hitMap.length && hitMap[shot] == 0) {
                        hitMap[shot] = 1;
                        huntSquares.remove(Integer.valueOf(shot));
                        return new int[]{shot / dim, shot % dim};
                    }
                }
            } catch (Exception e) {
                h = null; // fallback to parity hunt if Hunt fails
            }
        }

        // Optimized parity hunt: pick only squares that can fit remaining ships
        List<Integer> candidates = new ArrayList<>();
        for (int pos : huntSquares) {
            if (hitMap[pos] == 0 && canFitShip(pos)) candidates.add(pos);
        }
        if (candidates.isEmpty()) {
            // fallback to any remaining unhit square
            for (int i = 0; i < hitMap.length; i++) {
                if (hitMap[i] == 0) candidates.add(i);
            }
        }

        int idx = ThreadLocalRandom.current().nextInt(candidates.size());
        shot = candidates.get(idx);
        huntSquares.remove(Integer.valueOf(shot));
        hitMap[shot] = 1;

        return new int[]{shot / dim, shot % dim};
    }
}
