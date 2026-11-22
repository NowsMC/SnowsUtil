package space.nows.util.math;

/** Branch-free absolute value and sign. */
public final class Signs {
    private Signs() {}

    public static int abs(int v) { int m = v >> 31; return (v ^ m) - m; }

    public static long abs(long v) { long m = v >> 63; return (v ^ m) - m; }

    public static float abs(float v) { return Float.intBitsToFloat(Float.floatToRawIntBits(v) & 0x7FFFFFFF); }

    public static double abs(double v) { return Double.longBitsToDouble(Double.doubleToRawLongBits(v) & 0x7FFFFFFFFFFFFFFFL); }

    /** {@code -1}, {@code 0} or {@code 1}. */
    public static int sign(int v) { return (v >> 31) | (-v >>> 31); }

    /** {@code -1}, {@code 0} or {@code 1}; NaN gives 0. */
    public static int sign(float v) { return v > 0f ? 1 : (v < 0f ? -1 : 0); }
}
