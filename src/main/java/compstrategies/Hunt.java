package compstrategies;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class Hunt {
    private int dim;
    private int lastShot = -1;
    private boolean horizontal = false;
    private boolean orientationKnown = false;
    private Queue<Integer> targetQueue;

    public Hunt(int hitOne, int dim) {
        this.dim = dim;
        this.targetQueue = new LinkedList<>();
        // Initially, add only valid neighbors
        int row = hitOne / dim;
        int col = hitOne % dim;
        if (row + 1 < dim) targetQueue.add(hitOne + dim);
        if (row - 1 >= 0) targetQueue.add(hitOne - dim);
        if (col + 1 < dim) targetQueue.add(hitOne + 1);
        if (col - 1 >= 0) targetQueue.add(hitOne - 1);
        lastShot = hitOne;
    }

    public int huntShip(int[] hitMap) throws IOException {
        while (!targetQueue.isEmpty()) {
            int next = targetQueue.poll();
            if (next < 0 || next >= hitMap.length) continue;
            if (hitMap[next] != 0) continue;

            int rowDiff = Math.abs(next / dim - lastShot / dim);
            int colDiff = Math.abs(next % dim - lastShot % dim);

            // Determine orientation immediately if possible
            if (!orientationKnown) {
                if (rowDiff == 0 && colDiff == 1) horizontal = true;
                else if (rowDiff == 1 && colDiff == 0) horizontal = false;
                orientationKnown = true;
            }

            lastShot = next;

            // Extend in line first
            if (orientationKnown) {
                int forward = horizontal ? next + 1 : next + dim;
                int backward = horizontal ? next - 1 : next - dim;

                if (forward >= 0 && forward < hitMap.length && hitMap[forward] == 0) targetQueue.add(forward);
                else if (backward >= 0 && backward < hitMap.length && hitMap[backward] == 0) targetQueue.add(backward);
            } else {
                // continue exploring adjacent hits if orientation unknown
                int row = next / dim;
                int col = next % dim;
                if (row + 1 < dim && hitMap[next + dim] == 0) targetQueue.add(next + dim);
                if (row - 1 >= 0 && hitMap[next - dim] == 0) targetQueue.add(next - dim);
                if (col + 1 < dim && hitMap[next + 1] == 0) targetQueue.add(next + 1);
                if (col - 1 >= 0 && hitMap[next - 1] == 0) targetQueue.add(next - 1);
            }

            return next;
        }

        throw new IOException("No valid shots left in Hunt mode");
    }

    public void reset() {
        targetQueue.clear();
        lastShot = -1;
        orientationKnown = false;
    }
}
