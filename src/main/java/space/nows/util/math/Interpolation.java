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

    /** Maps {@code v} from {@code [inMin, inMax]} to {@code [outMin, outMax]} (not clamped). A degenerate input range yields {@code outMin}. */
    public static float remap(float v, float inMin, float inMax, float outMin, float outMax) {
        return lerp(outMin, outMax, inverseLerp(inMin, inMax, v));
    }

    /** Interpolates along the shortest arc between two angles in radians. */
    public static float lerpAngle(float a, float b, float t) {
        float d = FastMath.mod(b - a, FastMath.TWO_PI);
        if (d > FastMath.PI) d -= FastMath.TWO_PI;
        return a + d * t;
    }
}
