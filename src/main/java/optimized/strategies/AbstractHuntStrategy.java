package optimized.strategies;

import java.util.Arrays;
import java.util.random.RandomGenerator;
import optimized.engine.ShotResult;

abstract class AbstractHuntStrategy implements OptimizedStrategy {
    protected final byte[] hitMap = new byte[100];
    protected final int[] remaining = new int[6];
    protected final int[] candidates = new int[100];
    protected final HuntState hunt = new HuntState(hitMap);
    protected RandomGenerator random;

    @Override
    public void reset(RandomGenerator random) {
        this.random = random;
        Arrays.fill(hitMap, (byte) 0);
        Arrays.fill(remaining, 0);
        remaining[5] = 1;
        remaining[4] = 1;
        remaining[3] = 2;
        remaining[2] = 1;
        hunt.clear();
        resetSearchState();
    }

    protected abstract void resetSearchState();

    protected int chooseHuntShot() {
        if (!hunt.isActive()) return -1;
        int shot = hunt.next();
        if (shot >= 0) hitMap[shot] = 1;
        return shot;
    }

    @Override
    public void trackShot(int position, int result, long sunkLo, long sunkHi) {
        boolean hit = ShotResult.isHit(result);
        hitMap[position] = hit ? (byte) 2 : (byte) 1;
        if (hit) {
            if (!hunt.isActive()) hunt.start(position);
            hunt.registerHit(position);
        }
        int sunkLength = ShotResult.sunkLength(result);
        if (sunkLength != 0) {
            remaining[sunkLength]--;
            hunt.clear();
        }
    }

    protected boolean canFitRemainingShip(int position) {
        int row = position / 10;
        int col = position % 10;
        for (int length = 2; length <= 5; length++) {
            if (remaining[length] != 0 && (length <= 10 - col || length <= 10 - row)) return true;
        }
        return false;
    }
}
