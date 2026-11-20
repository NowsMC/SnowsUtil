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
}
