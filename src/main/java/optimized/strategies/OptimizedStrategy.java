package optimized.strategies;

import java.util.random.RandomGenerator;

/** Strategies receive observations only; hidden fleet masks are never exposed. */
public interface OptimizedStrategy {
    void reset(RandomGenerator random);

    int chooseShot();

    void trackShot(int position, int result, long sunkLo, long sunkHi);

    String name();
}
