package space.nows.util.math;

/** Sign of a number as {@code -1}, {@code 0} or {@code 1}. */
public final class Signs {
    private Signs() {}

    /** {@code -1}, {@code 0} or {@code 1}. */
    public static int sign(int v) { return (v >> 31) | (-v >>> 31); }

    /** {@code -1}, {@code 0} or {@code 1}; NaN gives 0. */
    public static int sign(float v) { return v > 0f ? 1 : (v < 0f ? -1 : 0); }
}
