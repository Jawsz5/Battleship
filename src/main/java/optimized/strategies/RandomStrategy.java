package optimized.strategies;

import java.util.random.RandomGenerator;

/** Uniform random selection using a reusable swap-remove array. */
public final class RandomStrategy implements OptimizedStrategy {
    private final int[] available = new int[100];
    private int count;
    private RandomGenerator random;

    @Override
    public void reset(RandomGenerator random) {
        this.random = random;
        count = available.length;
        for (int cell = 0; cell < available.length; cell++) available[cell] = cell;
    }

    @Override
    public int chooseShot() {
        if (count == 0) throw new IllegalStateException("No squares left");
        int index = random.nextInt(count);
        int shot = available[index];
        available[index] = available[--count];
        return shot;
    }

    @Override
    public void trackShot(int position, int result, long sunkLo, long sunkHi) {}

    @Override
    public String name() { return "Random"; }
}
