package optimized.strategies;

import optimized.engine.BitBoard100;

/** Legacy even-checkerboard search policy using bitboard candidate filtering. */
public final class ParityStrategy extends AbstractHuntStrategy {
    private static final long EVEN_LO;
    private static final long EVEN_HI;

    static {
        long lo = 0L;
        long hi = 0L;
        for (int cell = 0; cell < 100; cell++) {
            if (((cell / 10 + cell % 10) & 1) == 0) {
                lo |= BitBoard100.lowMask(cell);
                hi |= BitBoard100.highMask(cell);
            }
        }
        EVEN_LO = lo;
        EVEN_HI = hi;
    }

    @Override
    protected void resetSearchState() {}

    @Override
    public int chooseShot() {
        int shot = chooseHuntShot();
        if (shot >= 0) return shot;

        long candidateLo = 0L;
        long candidateHi = 0L;
        for (int cell = 0; cell < 100; cell++) {
            if (hitMap[cell] == 0 && ((cell / 10 + cell % 10) & 1) == 0 && canFitRemainingShip(cell)) {
                candidateLo |= BitBoard100.lowMask(cell);
                candidateHi |= BitBoard100.highMask(cell);
            }
        }
        candidateLo &= EVEN_LO;
        candidateHi &= EVEN_HI;
        int count = BitBoard100.bitCount(candidateLo, candidateHi);
        if (count == 0) {
            for (int cell = 0; cell < 100; cell++) {
                if (hitMap[cell] == 0) {
                    candidateLo |= BitBoard100.lowMask(cell);
                    candidateHi |= BitBoard100.highMask(cell);
                }
            }
            count = BitBoard100.bitCount(candidateLo, candidateHi);
        }
        if (count == 0) throw new IllegalStateException("No squares left");
        shot = BitBoard100.nthSetCell(candidateLo, candidateHi, random.nextInt(count));
        hitMap[shot] = 1;
        return shot;
    }

    @Override
    public String name() { return "Parity"; }
}
