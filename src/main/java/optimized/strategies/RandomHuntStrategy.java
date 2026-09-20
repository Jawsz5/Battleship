package optimized.strategies;

import java.util.Arrays;

/** Legacy RandomHunt semantics with primitive candidate storage. */
public final class RandomHuntStrategy extends AbstractHuntStrategy {
    private final boolean[] available = new boolean[100];
    private int availableCount;

    @Override
    protected void resetSearchState() {
        Arrays.fill(available, true);
        availableCount = 100;
    }

    @Override
    public int chooseShot() {
        int shot = chooseHuntShot();
        if (shot >= 0) {
            remove(shot);
            return shot;
        }
        if (availableCount == 0) throw new IllegalStateException("No squares left");

        int count = 0;
        for (int position = 0; position < 100; position++) {
            if (available[position] && canFitRemainingShip(position)) candidates[count++] = position;
        }
        if (count == 0) {
            for (int position = 0; position < 100; position++) {
                if (available[position]) candidates[count++] = position;
            }
        }
        shot = candidates[random.nextInt(count)];
        remove(shot);
        hitMap[shot] = 1;
        return shot;
    }

    private void remove(int position) {
        if (available[position]) {
            available[position] = false;
            availableCount--;
        }
    }

    @Override
    public String name() { return "RandomHunt"; }
}
