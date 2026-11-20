package space.nows.util.math;

/** Linear interpolation and smoothstep curves. */
public final class Interpolation {
    private Interpolation() {}

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
        float t = FastMath.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float smootherstep(float edge0, float edge1, float x) {
        if (edge0 == edge1) return x < edge0 ? 0f : 1f;
        float t = FastMath.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }
}
