package space.nows.util.math.geometry;

/** Whether a point lies inside a rectangle or circle (edges included). */
public final class Containment {
    private Containment() {}

    public static boolean pointInRect(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public static boolean pointInCircle(float px, float py, float cx, float cy, float r) {
        return Distance.distanceSq(px, py, cx, cy) <= r * r;
    }
}
