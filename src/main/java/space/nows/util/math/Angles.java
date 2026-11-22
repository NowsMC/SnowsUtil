package space.nows.util.math;

/** Angle constants and conversions between degrees and radians (single precision). */
public final class Angles {
    private Angles() {}

    public static final float PI = (float) Math.PI;
    public static final float TWO_PI = PI * 2f;
    public static final float HALF_PI = PI * 0.5f;
    public static final float DEG_TO_RAD = PI / 180f;
    public static final float RAD_TO_DEG = 180f / PI;

    public static float toRadians(float degrees) { return degrees * DEG_TO_RAD; }

    public static float toDegrees(float radians) { return radians * RAD_TO_DEG; }
}
