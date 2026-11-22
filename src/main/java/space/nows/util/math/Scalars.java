package space.nows.util.math;

/** Approximate comparison and squaring of scalars. */
public final class Scalars {
    private Scalars() {}

    public static boolean approxEquals(float a, float b, float epsilon) { return Signs.abs(a - b) <= epsilon; }

    public static float square(float v) { return v * v; }
}
