package optimized.probability;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorOperators;

/** Narrow Vector API proof-of-concept for dense score-array arithmetic; not part of the canonical runner. */
public final class VectorApiExperiment {
    public static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    private VectorApiExperiment() {}

    public static int vectorWidth() { return SPECIES.length(); }

    public static long scalarAddChecksum(int[] left, int[] right, int[] output) {
        long checksum = 0;
        for (int i = 0; i < left.length; i++) {
            int value = left[i] + right[i];
            output[i] = value;
            checksum += value;
        }
        return checksum;
    }

    public static long vectorAddChecksum(int[] left, int[] right, int[] output) {
        long checksum = 0;
        int i = 0;
        int upper = SPECIES.loopBound(left.length);
        for (; i < upper; i += SPECIES.length()) {
            IntVector values = IntVector.fromArray(SPECIES, left, i)
                    .add(IntVector.fromArray(SPECIES, right, i));
            values.intoArray(output, i);
            checksum += values.reduceLanes(VectorOperators.ADD);
        }
        for (; i < left.length; i++) {
            int value = left[i] + right[i];
            output[i] = value;
            checksum += value;
        }
        return checksum;
    }
}
