package space.nows.util.math.geometry;

/** Squared and plain distance in 2D and 3D. */
public final class Distance {
    private Distance() {}

    public static float distanceSq(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(distanceSq(x1, y1, x2, y2));
    }

    public static float distanceSq(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }
}
