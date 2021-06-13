package awais.instagrabber.utils;

import java.util.Arrays;

public class CubicInterpolation {

    private final float[] array;
    private final int tangentFactor;
    private final int length;

    public CubicInterpolation(final float[] array, final int cubicTension) {
        this.array = Arrays.copyOf(array, array.length);
        this.length = array.length;
        tangentFactor = 1 - Math.max(0, Math.min(1, cubicTension));
    }

    public CubicInterpolation(final float[] array) {
        this(array, 0);
    }

    private float getTangent(int k) {
        return tangentFactor * (getClippedInput(k + 1) - getClippedInput(k - 1)) / 2;
    }

    public float interpolate(final float t) {
        int k = (int) Math.floor(t);
        float[] m = new float[]{getTangent(k), getTangent(k + 1)};
        float[] p = new float[]{getClippedInput(k), getClippedInput(k + 1)};
        final float t1 = t - k;
        final float t2 = t1 * t1;
        final float t3 = t1 * t2;
        return (2 * t3 - 3 * t2 + 1) * p[0] + (t3 - 2 * t2 + t1) * m[0] + (-2 * t3 + 3 * t2) * p[1] + (t3 - t2) * m[1];
    }

    private float getClippedInput(int i) {
        if (i >= 0 && i < length) {
            return array[i];
        }
        return array[clipClamp(i, length)];
    }

    private int clipClamp(int i, int n) {
        return Math.max(0, Math.min(i, n - 1));
    }
}
