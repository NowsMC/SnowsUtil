package space.nows.util.math;

/** Clamping to a range and approximate equality of floating point values. */
public final class Bounds {
    private Bounds() {}

    public static int clamp(int v, int min, int max) { return v < min ? min : (v > max ? max : v); }

    public static long clamp(long v, long min, long max) { return v < min ? min : (v > max ? max : v); }

    public static float clamp(float v, float min, float max) { return v < min ? min : (v > max ? max : v); }

    public static double clamp(double v, double min, double max) { return v < min ? min : (v > max ? max : v); }

    public static boolean approxEquals(float a, float b, float epsilon) { return Math.abs(a - b) <= epsilon; }

    public static boolean approxEquals(double a, double b, double epsilon) { return Math.abs(a - b) <= epsilon; }
}
