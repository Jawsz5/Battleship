package compstrategies.standardstrats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import compstrategies.Hunt;

public class RandomHuntStrat extends RandomStrat {
    private byte[] hitMap;                  // 0 = unknown, 1 = miss, 2 = hit
    private Hunt h;
    private List<Integer> remainingShipLengths;

    public RandomHuntStrat(int mapDim) {
        super(mapDim);
        hitMap = new byte[nCells];
        for (int i = 0; i < nCells; i++) hitMap[i] = 0;

        // Track standard ship lengths internally
        remainingShipLengths = new ArrayList<>();
        remainingShipLengths.add(5);
        remainingShipLengths.add(4);
        remainingShipLengths.add(3);
        remainingShipLengths.add(3);
        remainingShipLengths.add(2);
    }

    @Override
    public void trackShot(boolean hit, int sunkLen, int pos, int[] sunkCells) {
        hitMap[pos] = (byte) (hit ? 2 : 1);

        if (hit) {
            if (h == null) {
                h = new Hunt(pos, dim); // start Hunt mode
            }
            // Let Hunt extend aggressively
            h.registerHit(pos, hitMap);
        }

        if (sunkLen != 0) {
            remainingShipLengths.remove(Integer.valueOf(sunkLen));
            if (h != null) {
                h.reset();
                h = null;
            }
        }
    }

    @Override
    public int selectShot() {
        int shot = -1;

        // Hunt mode first
        if (h != null) {
            try {
                while (true) {
                    shot = h.huntShip(hitMap);
                    if (shot >= 0 && shot < hitMap.length && hitMap[shot] == 0) {
                        hitMap[shot] = 1;
                        notShotAtSpots.remove(Integer.valueOf(shot));
                        return shot;
                    }
                }
            } catch (Exception e) {
                h = null; // fallback if Hunt fails
            }
        }

        // Random mode with pruning
        if (notShotAtSpots.isEmpty()) throw new IllegalStateException("No squares left");
        List<Integer> candidates = new ArrayList<>();

        for (int pos : notShotAtSpots) {
            int row = pos / dim;
            int col = pos % dim;
            boolean canFit = false;

            for (int len : remainingShipLengths) {
                if (len <= dim - col || len <= dim - row) { // horizontal or vertical fit
                    canFit = true;
                    break;
                }
            }

            if (canFit) candidates.add(pos);
        }

        if (candidates.isEmpty()) candidates = new ArrayList<>(notShotAtSpots);

        int idx = ThreadLocalRandom.current().nextInt(candidates.size());
        shot = candidates.get(idx);
        notShotAtSpots.remove(Integer.valueOf(shot));
        hitMap[shot] = 1;

        return shot;
    }
}


