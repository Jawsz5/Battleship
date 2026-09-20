package optimized.strategies;

/** Primitive ordered frontier preserving the legacy Hunt traversal rules. */
final class HuntState {
    private final byte[] hitMap;
    private final int[] frontier = new int[1024];
    private int head;
    private int tail;
    private int origin;
    private boolean oriented;
    private boolean active;

    HuntState(byte[] hitMap) {
        this.hitMap = hitMap;
    }

    void clear() {
        head = tail = 0;
        oriented = false;
        active = false;
    }

    boolean isActive() { return active; }

    void start(int position) {
        origin = position;
        head = tail = 0;
        oriented = false;
        active = true;
        addNeighbors(position);
    }

    int next() {
        while (head < tail) {
            int position = frontier[head++];
            if (hitMap[position] == 0) return position;
        }
        clear();
        return -1;
    }

    void registerHit(int position) {
        int row = position / 10;
        int col = position % 10;
        if (!oriented) {
            if (Math.abs(origin / 10 - row) == 1 && origin % 10 == col) {
                oriented = true;
                extendVertical(origin);
                extendVertical(position);
            } else if (Math.abs(origin % 10 - col) == 1 && origin / 10 == row) {
                oriented = true;
                extendHorizontal(origin);
                extendHorizontal(position);
            } else {
                addNeighbors(position);
            }
        } else if (origin / 10 == row) {
            extendHorizontal(position);
        } else if (origin % 10 == col) {
            extendVertical(position);
        }
    }

    private void addNeighbors(int position) {
        int row = position / 10;
        int col = position % 10;
        if (row > 0) add((row - 1) * 10 + col);
        if (row < 9) add((row + 1) * 10 + col);
        if (col > 0) add(row * 10 + col - 1);
        if (col < 9) add(row * 10 + col + 1);
    }

    private void extendHorizontal(int position) {
        int row = position / 10;
        int col = position % 10;
        for (int c = col - 1; c >= 0; c--) {
            int cell = row * 10 + c;
            if (hitMap[cell] == 1) break;
            if (hitMap[cell] == 0) add(cell);
        }
        for (int c = col + 1; c < 10; c++) {
            int cell = row * 10 + c;
            if (hitMap[cell] == 1) break;
            if (hitMap[cell] == 0) add(cell);
        }
    }

    private void extendVertical(int position) {
        int row = position / 10;
        int col = position % 10;
        for (int r = row - 1; r >= 0; r--) {
            int cell = r * 10 + col;
            if (hitMap[cell] == 1) break;
            if (hitMap[cell] == 0) add(cell);
        }
        for (int r = row + 1; r < 10; r++) {
            int cell = r * 10 + col;
            if (hitMap[cell] == 1) break;
            if (hitMap[cell] == 0) add(cell);
        }
    }

    private void add(int cell) {
        if (tail == frontier.length && head > 0) {
            int size = tail - head;
            System.arraycopy(frontier, head, frontier, 0, size);
            head = 0;
            tail = size;
        }
        if (tail == frontier.length) throw new IllegalStateException("Hunt frontier capacity exceeded");
        frontier[tail++] = cell;
    }
}
