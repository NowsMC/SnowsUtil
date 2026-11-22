package space.nows.util.math.geometry;

/** Whether two line segments in 2D share at least one point. */
public final class SegmentIntersection {
    private SegmentIntersection() {}

    /** Whether segment A-B and segment C-D share at least one point (touching and collinear overlap count). */
    public static boolean segmentsIntersect(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
        float d1 = orient(cx, cy, dx, dy, ax, ay);
        float d2 = orient(cx, cy, dx, dy, bx, by);
        float d3 = orient(ax, ay, bx, by, cx, cy);
        float d4 = orient(ax, ay, bx, by, dx, dy);
        if (((d1 > 0f && d2 < 0f) || (d1 < 0f && d2 > 0f)) && ((d3 > 0f && d4 < 0f) || (d3 < 0f && d4 > 0f))) return true;
        return (d1 == 0f && onSegment(cx, cy, dx, dy, ax, ay))
            || (d2 == 0f && onSegment(cx, cy, dx, dy, bx, by))
            || (d3 == 0f && onSegment(ax, ay, bx, by, cx, cy))
            || (d4 == 0f && onSegment(ax, ay, bx, by, dx, dy));
    }

    private static float orient(float px, float py, float qx, float qy, float rx, float ry) {
        return (qx - px) * (ry - py) - (qy - py) * (rx - px);
    }

    private static boolean onSegment(float px, float py, float qx, float qy, float rx, float ry) {
        return rx >= Math.min(px, qx) && rx <= Math.max(px, qx) && ry >= Math.min(py, qy) && ry <= Math.max(py, qy);
    }
}
