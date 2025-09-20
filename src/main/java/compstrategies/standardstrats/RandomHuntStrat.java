package compstrategies.standardstrats;

import java.util.*;

/**
 * RandomHuntStrat
 * - Hunt: picks uniformly at random from all unshot cells.
 * - Target: once we hit, we sweep orthogonally to finish the ship fast.
 *
 * Conventions:
 *   hitMap: 0=unknown, 1=miss, 2=hit
 *   Coordinates: x=row, y=col
 */
public final class RandomHuntStrat {
    private final int dim, nCells;
    private final Random rng = new Random();

    // Board state (owned by the strategy, fed by trackShot)
    private final byte[] hitMap;               // 0=unknown,1=miss,2=hit
    private final int[] free;                  // list of unshot cell ids (swap-erase)
    private final int[] pos;                   // position of cell id in free[], -1 if already shot
    private int freeSize;

    // Remaining ship lengths (optional, used only for bookkeeping / UI)
    private final ArrayList<Integer> remaining = new ArrayList<>();

    // Simple target-mode state
    private boolean inTarget = false;
    private int anchorId = -1;                 // first hit cell id of the current target
    private boolean oriented = false;          // true when we know row or column
    private boolean orientRow = false;         // true if orientation is horizontal (same row)
    private int minRow, maxRow, minCol, maxCol; // bounds of the contiguous hit segment we’re sweeping

    public RandomHuntStrat(int dimension) {
        this.dim = dimension;
        this.nCells = dim * dim;

        this.hitMap = new byte[nCells];
        this.free = new int[nCells];
        this.pos = new int[nCells];
        for (int i = 0; i < nCells; i++) {
            free[i] = i;
            pos[i] = i;
        }
        freeSize = nCells;

        // Default classic fleet (adjust if your engine provides this differently)
        // Using it only for nicer neighbor-blocking if you want; algorithm doesn’t depend on it.
        int[] fleet = (dim == 10) ? new int[]{5,4,3,3,2} : new int[]{Math.min(dim,5)};
        for (int L : fleet) remaining.add(L);
    }

    public String name() { return "RandomHunt+Target"; }

    /** Selects the next shot: returns {x(row), y(col)} */
    public int[] selectShot() {
        if (inTarget) {
            int next = pickTargetCell();
            if (next >= 0) {
                removeIdFromFree(next);                    // <-- NEW: keep pools in sync
                return new int[]{ next / dim, next % dim };
            }
            resetTarget();
        }
        int id = drawRandomFree();
        return new int[]{ id / dim, id % dim };
    }
    

    /**
     * Informs the strategy of the outcome of the previously fired shot.
     * @param hit     true if it was a hit
     * @param sunkLen length of the ship just sunk (0 if not sunk)
     * @param x       row of the shot
     * @param y       col of the shot
     */
    public void trackShot(boolean hit, int sunkLen, int x, int y) {
        final int id = x * dim + y;

        // Mark result now (we did NOT pre-mark in selectShot)
        hitMap[id] = hit ? (byte)2 : (byte)1;

        if (!hit) {
            // If we were sweeping a line and this misses, switch to the opposite end
            return;
        }

        // Hit logic
        if (!inTarget) {
            enterTarget(id);
        } else {
            // Already in target mode—expand bounds and try to deduce orientation
            extendBoundsWith(id);
            if (!oriented) tryLockOrientation();
        }

        // Handle sink: clear target state and remove length from remaining list
        if (sunkLen > 0) {
            remaining.remove(Integer.valueOf(sunkLen));
            resetTarget();
        }
    }

    // ====================== Target Mode Helpers ======================

    private void enterTarget(int id) {
        inTarget = true;
        anchorId = id;
        oriented = false;
        // Initialize bounds to the single hit
        int r = id / dim, c = id % dim;
        minRow = maxRow = r;
        minCol = maxCol = c;
    }

    private void resetTarget() {
        inTarget = false;
        anchorId = -1;
        oriented = false;
    }

    private void extendBoundsWith(int id) {
        int r = id / dim, c = id % dim;
        if (r < minRow) minRow = r;
        if (r > maxRow) maxRow = r;
        if (c < minCol) minCol = c;
        if (c > maxCol) maxCol = c;
    }

    private void tryLockOrientation() {
        // If we have 2 hits in same row or same column, orientation is known
        if (minRow == maxRow && (maxCol - minCol) >= 1) {
            oriented = true;
            orientRow = true;  // horizontal
        } else if (minCol == maxCol && (maxRow - minRow) >= 1) {
            oriented = true;
            orientRow = false; // vertical
        }
    }

    /** Returns next target cell id, or -1 if none available. */
    private int pickTargetCell() {
        // If orientation unknown: probe orthogonal neighbors of the whole current bounds, randomly
        if (!oriented) {
            ArrayList<Integer> opts = new ArrayList<>(4);
            int r = anchorId / dim, c = anchorId % dim;

            pushIfShootable(r - 1, c, opts);
            pushIfShootable(r + 1, c, opts);
            pushIfShootable(r, c - 1, opts);
            pushIfShootable(r, c + 1, opts);

            if (opts.isEmpty()) return -1;
            return opts.get(rng.nextInt(opts.size()));
        }

        // Oriented: extend from both ends (random order to avoid bias)
        if (orientRow) {
            int left  = toId(minRow, minCol - 1);
            int right = toId(minRow, maxCol + 1);
            return drawFirstShootableRandomly(left, right);
        } else {
            int up   = toId(minRow - 1, minCol);
            int down = toId(maxRow + 1, minCol);
            return drawFirstShootableRandomly(up, down);
        }
    }

    private int drawFirstShootableRandomly(int a, int b) {
        int ra = rng.nextBoolean() ? 0 : 1;
        int[] order = (ra == 0) ? new int[]{a, b} : new int[]{b, a};
        for (int id : order) if (isShootableId(id)) return id;
        return -1;
    }

    private void pushIfShootable(int r, int c, List<Integer> out) {
        int id = toId(r, c);
        if (isShootableId(id)) out.add(id);
    }

    private boolean isShootableId(int id) {
        if (id < 0 || id >= nCells) return false;
        return hitMap[id] == 0; // unknown only
    }

    private int toId(int r, int c) {
        if (r < 0 || r >= dim || c < 0 || c >= dim) return -1;
        return r * dim + c;
    }

    // ====================== Hunt (Random) Helpers ======================

    /** Draw a random unshot cell id in O(1) using swap-erase. */
    private int drawRandomFree() {
        // Compact free[] as we mark cells shot in trackShot()
        // But we only remove when we actually fire, so do it here now:
        int pickIdx = rng.nextInt(freeSize);
        int id = free[pickIdx];

        // Ensure we don’t return something already shot (can happen if external code
        // replays results without calling selectShot()—defensive check)
        if (hitMap[id] != 0) {
            // Find a fresh one by walking backward; this is rare and still O(1) amortized
            while (freeSize > 0 && hitMap[free[freeSize - 1]] != 0) {
                // hard-delete tail
                int tailId = free[freeSize - 1];
                pos[tailId] = -1;
                freeSize--;
            }
            if (freeSize == 0) return 0; // degenerate fallback

            pickIdx = rng.nextInt(freeSize);
            id = free[pickIdx];
        }

        // Remove id from the free pool now (we *are* firing this cell)
        swapEraseFree(pickIdx);
        return id;
    }

    /** Removes free[index] from the pool in O(1). */
    private void swapEraseFree(int index) {
        int lastIdx = freeSize - 1;
        int removedId = free[index];
        int lastId = free[lastIdx];

        free[index] = lastId;
        pos[lastId] = index;

        free[lastIdx] = removedId;
        pos[removedId] = -1;

        freeSize--;
    }

    // Remove a specific cell id from the free[] pool in O(1), if it’s still there.
    private void removeIdFromFree(int id) {
        int index = (id >= 0 && id < nCells) ? pos[id] : -1;
        if (index >= 0 && index < freeSize) {
            swapEraseFree(index);
        }
    }

}
