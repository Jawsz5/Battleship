package optimized;

import optimized.engine.BitBoard100;

final class TestFleet {
    private TestFleet() {}

    static long[] low() {
        return new long[] {low(0, 1, 2, 3, 4), low(6, 7, 8, 9), low(10, 20, 30),
                low(63, 64, 65), low(90, 91)};
    }

    static long[] high() {
        return new long[] {high(0, 1, 2, 3, 4), high(6, 7, 8, 9), high(10, 20, 30),
                high(63, 64, 65), high(90, 91)};
    }

    static long low(int... cells) {
        long result = 0L;
        for (int cell : cells) result |= BitBoard100.lowMask(cell);
        return result;
    }

    static long high(int... cells) {
        long result = 0L;
        for (int cell : cells) result |= BitBoard100.highMask(cell);
        return result;
    }
}
