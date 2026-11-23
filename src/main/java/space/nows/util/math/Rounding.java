package space.nows.util.math;

/**
 * Conversion of floating-point values to integers: floor, ceiling and round-half-up. Infinite and out-of-range
 * values saturate at the {@code int} range. NaN has no integer value and is rejected with {@link IllegalArgumentException}.
 */
public final class Rounding {
    private Rounding() {}

    public static int floor(float v) { return (int) Math.floor(nonNaN(v)); }

    public static int floor(double v) { return (int) Math.floor(nonNaN(v)); }

    public static int ceil(float v) { return (int) Math.ceil(nonNaN(v)); }

    public static int ceil(double v) { return (int) Math.ceil(nonNaN(v)); }

    /** Round half up; exact (unlike {@code floor(v + 0.5f)}, which misrounds 0.49999997f) because it is the JDK's {@link Math#round(float)}. */
    public static int round(float v) { return Math.round(nonNaN(v)); }

    /** Round half up, saturating at the {@code int} range. */
    public static int round(double v) {
        long r = Math.round(nonNaN(v));
        return r > Integer.MAX_VALUE ? Integer.MAX_VALUE : (r < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) r);
    }

    private static float nonNaN(float v) {
        if (Float.isNaN(v)) throw new IllegalArgumentException("NaN has no integer value");
        return v;
    }

    private static double nonNaN(double v) {
        if (Double.isNaN(v)) throw new IllegalArgumentException("NaN has no integer value");
        return v;
    }
}
