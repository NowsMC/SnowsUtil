package space.nows.util.math;

/** Linear interpolation and smoothstep curves. */
public final class Interpolation {
    private Interpolation() {}

    /** Exact at both ends: {@code lerp(a, b, 0) == a} and {@code lerp(a, b, 1) == b}; monotonic in {@code t}. */
    public static float lerp(float a, float b, float t) {
        if (t == 0f) return a;
        if (t == 1f) return b;
        return t < 0.5f ? a + (b - a) * t : b - (b - a) * (1f - t);
    }

    /** Exact at both ends: {@code lerp(a, b, 0) == a} and {@code lerp(a, b, 1) == b}; monotonic in {@code t}. */
    public static double lerp(double a, double b, double t) {
        if (t == 0.0) return a;
        if (t == 1.0) return b;
        return t < 0.5 ? a + (b - a) * t : b - (b - a) * (1.0 - t);
    }

    /** Returns {@code (value - a) / (b - a)}, or 0 when {@code a == b} because the ratio is undefined. */
    public static float inverseLerp(float a, float b, float value) {
        return a == b ? 0f : (value - a) / (b - a);
    }

    /** Returns {@code (value - a) / (b - a)}, or 0 when {@code a == b} because the ratio is undefined. */
    public static double inverseLerp(double a, double b, double value) {
        return a == b ? 0d : (value - a) / (b - a);
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        if (edge0 == edge1) return x < edge0 ? 0f : 1f;
        float t = Bounds.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float smootherstep(float edge0, float edge1, float x) {
        if (edge0 == edge1) return x < edge0 ? 0f : 1f;
        float t = Bounds.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    /** Maps {@code v} from {@code [inMin, inMax]} to {@code [outMin, outMax]} (not clamped). A degenerate input range yields {@code outMin}. */
    public static float remap(float v, float inMin, float inMax, float outMin, float outMax) {
        return lerp(outMin, outMax, inverseLerp(inMin, inMax, v));
    }

    /** Interpolates along the shortest arc between two angles in radians. */
    public static float lerpAngle(float a, float b, float t) {
        float d = Modulo.mod(b - a, Angles.TWO_PI);
        if (d > Angles.PI) d -= Angles.TWO_PI;
        return a + d * t;
    }
}
