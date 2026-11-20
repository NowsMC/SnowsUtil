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

    public static int fastFloor(float v) { int i = (int) v; return v < i ? i - 1 : i; }

    public static int fastFloor(double v) { int i = (int) v; return v < i ? i - 1 : i; }

    public static int fastCeil(float v) { int i = (int) v; return v > i ? i + 1 : i; }

    public static int fastCeil(double v) { int i = (int) v; return v > i ? i + 1 : i; }

    public static int fastRound(float v) { return fastFloor(v + 0.5f); }

    public static int fastRound(double v) { return fastFloor(v + 0.5d); }

    public static int sign(int v) { return (v >> 31) | (-v >>> 31); }

    public static int sign(float v) { return v > 0f ? 1 : (v < 0f ? -1 : 0); }

    public static int mod(int a, int m) { int r = a % m; return r < 0 ? r + m : r; }

    public static float mod(float a, float m) { float r = a % m; return r < 0f ? r + m : r; }

    public static float wrap(float v, float min, float max) { return min + mod(v - min, max - min); }

    public static boolean approxEquals(float a, float b, float epsilon) { return fastAbs(a - b) <= epsilon; }

    public static float square(float v) { return v * v; }

    public static float toRadians(float degrees) { return degrees * DEG_TO_RAD; }

    public static float toDegrees(float radians) { return radians * RAD_TO_DEG; }
}
