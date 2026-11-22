package space.nows.util.math;

/** Clamping and the two-value minimum and maximum, written as plain comparisons for each primitive type. */
public final class Bounds {
    private Bounds() {}

    public static int clamp(int v, int min, int max) { return v < min ? min : (v > max ? max : v); }

    public static long clamp(long v, long min, long max) { return v < min ? min : (v > max ? max : v); }

    public static float clamp(float v, float min, float max) { return v < min ? min : (v > max ? max : v); }

    public static double clamp(double v, double min, double max) { return v < min ? min : (v > max ? max : v); }

    public static int min(int a, int b) { return a < b ? a : b; }

    public static long min(long a, long b) { return a < b ? a : b; }

    public static float min(float a, float b) { return a < b ? a : b; }

    public static double min(double a, double b) { return a < b ? a : b; }

    public static int max(int a, int b) { return a > b ? a : b; }

    public static long max(long a, long b) { return a > b ? a : b; }

    public static float max(float a, float b) { return a > b ? a : b; }

    public static double max(double a, double b) { return a > b ? a : b; }
}
