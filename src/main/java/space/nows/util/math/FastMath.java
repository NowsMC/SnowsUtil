package space.nows.util.math;

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

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    public static float inverseLerp(float a, float b, float value) {
        return a == b ? 0f : (value - a) / (b - a);
    }

    public static double inverseLerp(double a, double b, double value) {
        return a == b ? 0d : (value - a) / (b - a);
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        if (edge0 == edge1) return x < edge0 ? 0f : 1f;
        float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float smootherstep(float edge0, float edge1, float x) {
        if (edge0 == edge1) return x < edge0 ? 0f : 1f;
        float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    public static int fastFloor(float v) { int i = (int) v; return v < i ? i - 1 : i; }

    public static int fastFloor(double v) { int i = (int) v; return v < i ? i - 1 : i; }

    public static int fastCeil(float v) { int i = (int) v; return v > i ? i + 1 : i; }

    public static int fastCeil(double v) { int i = (int) v; return v > i ? i + 1 : i; }

    public static int fastRound(float v) { return fastFloor(v + 0.5f); }

    public static int fastRound(double v) { return fastFloor(v + 0.5d); }

    public static boolean isPowerOfTwo(int v) { return v > 0 && (v & (v - 1)) == 0; }

    public static int nextPowerOfTwo(int v) {
        if (v <= 1) return 1;
        if (v > (1 << 30)) throw new IllegalArgumentException("too large: " + v);
        return 1 << (32 - Integer.numberOfLeadingZeros(v - 1));
    }

    public static int log2(int v) {
        if (v <= 0) throw new IllegalArgumentException("v <= 0");
        return 31 - Integer.numberOfLeadingZeros(v);
    }

    public static int sign(int v) { return (v >> 31) | (-v >>> 31); }

    public static int sign(float v) { return v > 0f ? 1 : (v < 0f ? -1 : 0); }

    public static int mod(int a, int m) { int r = a % m; return r < 0 ? r + m : r; }

    public static float mod(float a, float m) { float r = a % m; return r < 0f ? r + m : r; }

    public static float wrap(float v, float min, float max) { return min + mod(v - min, max - min); }

    public static boolean approxEquals(float a, float b, float epsilon) { return fastAbs(a - b) <= epsilon; }

    public static float square(float v) { return v * v; }

    private static final int TABLE_BITS = 12;
    private static final int TABLE_SIZE = 1 << TABLE_BITS;
    private static final int TABLE_MASK = TABLE_SIZE - 1;
    private static final float TO_INDEX = TABLE_SIZE / TWO_PI;
    private static final float[] SIN_TABLE = new float[TABLE_SIZE];

    static {
        for (int i = 0; i < TABLE_SIZE; i++) {
            SIN_TABLE[i] = (float) Math.sin(i * (2.0 * Math.PI / TABLE_SIZE));
        }
    }

    public static float sin(float radians) { return SIN_TABLE[fastFloor(radians * TO_INDEX) & TABLE_MASK]; }

    public static float cos(float radians) { return SIN_TABLE[(fastFloor(radians * TO_INDEX) + (TABLE_SIZE >> 2)) & TABLE_MASK]; }

    public static float toRadians(float degrees) { return degrees * DEG_TO_RAD; }

    public static float toDegrees(float radians) { return radians * RAD_TO_DEG; }
}
