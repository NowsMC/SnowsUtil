package space.nows.util.math.geometry;

import space.nows.util.math.FastMath;

/** Point-in-shape and shape-overlap tests for rects, circles and boxes. */
public final class Intersections {
    private Intersections() {}

    public static boolean pointInRect(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public static boolean pointInCircle(float px, float py, float cx, float cy, float r) {
        return Distance.distanceSq(px, py, cx, cy) <= r * r;
    }

    public static boolean circlesOverlap(float x1, float y1, float r1, float x2, float y2, float r2) {
        float r = r1 + r2;
        return Distance.distanceSq(x1, y1, x2, y2) <= r * r;
    }

    public static boolean rectsOverlap(float x1, float y1, float w1, float h1,
                                       float x2, float y2, float w2, float h2) {
        return x1 <= x2 + w2 && x1 + w1 >= x2 && y1 <= y2 + h2 && y1 + h1 >= y2;
    }

    public static boolean circleRectOverlap(float cx, float cy, float r, float x, float y, float w, float h) {
        float nx = FastMath.clamp(cx, x, x + w);
        float ny = FastMath.clamp(cy, y, y + h);
        return Distance.distanceSq(cx, cy, nx, ny) <= r * r;
    }

    public static boolean boxesOverlap(float minX1, float minY1, float minZ1, float maxX1, float maxY1, float maxZ1,
                                       float minX2, float minY2, float minZ2, float maxX2, float maxY2, float maxZ2) {
        return minX1 <= maxX2 && maxX1 >= minX2
            && minY1 <= maxY2 && maxY1 >= minY2
            && minZ1 <= maxZ2 && maxZ1 >= minZ2;
    }

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
