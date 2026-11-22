package space.nows.util.math;

/** Modulo arithmetic and wrapping a value into a range. */
public final class Modulo {
    private Modulo() {}

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

    /** Wraps {@code v} into {@code [min, max)}. */
    public static float wrap(float v, float min, float max) {
        float r = min + mod(v - min, max - min);
        return r < max ? r : Math.nextDown(max);
    }
}
