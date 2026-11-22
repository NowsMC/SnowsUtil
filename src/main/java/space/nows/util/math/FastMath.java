package space.nows.util.math;

/** Branch-light scalar helpers: constants, clamp, min/max/abs, floor/ceil/round, sign, mod. */
public final class FastMath {
    private FastMath() {}

    public static final float PI = (float) Math.PI;
    public static final float TWO_PI = PI * 2f;
    public static final float HALF_PI = PI * 0.5f;
    public static final float DEG_TO_RAD = PI / 180f;
    public static final float RAD_TO_DEG = 180f / PI;

    public static int clamp(int v, int min, int max) { return v < min ? min : (v > max ? max : v); }

    public static long clamp(long v, long min, long max) { return v < min ? min : (v > max ? max : v); }

    public static float clamp(float v, float min, float max) { return v < min ? min : (v > max ? max : v); }

    public static double clamp(double v, double min, double max) { return v < min ? min : (v > max ? max : v); }

    public static int fastMin(int a, int b) { return a < b ? a : b; }

    public static int fastMax(int a, int b) { return a > b ? a : b; }

    public static long fastMin(long a, long b) { return a < b ? a : b; }

    public static long fastMax(long a, long b) { return a > b ? a : b; }

    public static float fastMin(float a, float b) { return a < b ? a : b; }

    public static float fastMax(float a, float b) { return a > b ? a : b; }

    public static double fastMin(double a, double b) { return a < b ? a : b; }

    public static double fastMax(double a, double b) { return a > b ? a : b; }

    public static int fastAbs(int v) { int m = v >> 31; return (v ^ m) - m; }

    public static long fastAbs(long v) { long m = v >> 63; return (v ^ m) - m; }

    public static float fastAbs(float v) { return Float.intBitsToFloat(Float.floatToRawIntBits(v) & 0x7FFFFFFF); }

    public static double fastAbs(double v) { return Double.longBitsToDouble(Double.doubleToRawLongBits(v) & 0x7FFFFFFFFFFFFFFFL); }

    public static int fastFloor(float v) { int i = (int) v; return (v < i && i != Integer.MIN_VALUE) ? i - 1 : i; }

    public static int fastFloor(double v) { int i = (int) v; return (v < i && i != Integer.MIN_VALUE) ? i - 1 : i; }

    public static int fastCeil(float v) { int i = (int) v; return (v > i && i != Integer.MAX_VALUE) ? i + 1 : i; }

    public static int fastCeil(double v) { int i = (int) v; return (v > i && i != Integer.MAX_VALUE) ? i + 1 : i; }

    /** Round half up; exact (unlike {@code floor(v + 0.5f)}, which misrounds 0.49999997f) because it is the JDK's {@link Math#round(float)}. */
    public static int fastRound(float v) { return Math.round(v); }

    /** Round half up, saturating at the {@code int} range. */
    public static int fastRound(double v) {
        long r = Math.round(v);
        return r > Integer.MAX_VALUE ? Integer.MAX_VALUE : (r < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) r);
    }

    public static int sign(int v) { return (v >> 31) | (-v >>> 31); }

    public static int sign(float v) { return v > 0f ? 1 : (v < 0f ? -1 : 0); }

    /** Floor modulo (the JDK's {@link Math#floorMod(int, int)}): the result has the sign of {@code m}. */
    public static int mod(int a, int m) { return Math.floorMod(a, m); }

    /** Floor modulo for {@code m > 0}: the result is always in {@code [0, m)}; NaN propagates. */
    public static float mod(float a, float m) {
        if (!(m > 0f)) throw new IllegalArgumentException("m must be > 0 but was " + m);
        float r = a % m;
        if (!(r < 0f)) return r;
        float s = r + m;
        return s < m ? s : Math.nextDown(m);
    }

    public static float wrap(float v, float min, float max) { return min + mod(v - min, max - min); }

    public static boolean approxEquals(float a, float b, float epsilon) { return fastAbs(a - b) <= epsilon; }

    public static float square(float v) { return v * v; }

    public static float toRadians(float degrees) { return degrees * DEG_TO_RAD; }

    public static float toDegrees(float radians) { return radians * RAD_TO_DEG; }
}
