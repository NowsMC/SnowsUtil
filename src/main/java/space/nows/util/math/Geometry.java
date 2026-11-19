package space.nows.util.math;

public final class Geometry {
    private Geometry() {}

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

    public static boolean pointInRect(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public static boolean pointInCircle(float px, float py, float cx, float cy, float r) {
        return distanceSq(px, py, cx, cy) <= r * r;
    }

    public static boolean circlesOverlap(float x1, float y1, float r1, float x2, float y2, float r2) {
        float r = r1 + r2;
        return distanceSq(x1, y1, x2, y2) <= r * r;
    }

    public static boolean rectsOverlap(float x1, float y1, float w1, float h1,
                                       float x2, float y2, float w2, float h2) {
        return x1 <= x2 + w2 && x1 + w1 >= x2 && y1 <= y2 + h2 && y1 + h1 >= y2;
    }

    public static boolean circleRectOverlap(float cx, float cy, float r, float x, float y, float w, float h) {
        float nx = FastMath.clamp(cx, x, x + w);
        float ny = FastMath.clamp(cy, y, y + h);
        return distanceSq(cx, cy, nx, ny) <= r * r;
    }

    public static boolean boxesOverlap(float minX1, float minY1, float minZ1, float maxX1, float maxY1, float maxZ1,
                                       float minX2, float minY2, float minZ2, float maxX2, float maxY2, float maxZ2) {
        return minX1 <= maxX2 && maxX1 >= minX2
            && minY1 <= maxY2 && maxY1 >= minY2
            && minZ1 <= maxZ2 && maxZ1 >= minZ2;
    }

    public static boolean pointInPolygon(float px, float py, float[] xy) {
        int n = xy.length >> 1;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            float xi = xy[i * 2], yi = xy[i * 2 + 1], xj = xy[j * 2], yj = xy[j * 2 + 1];
            if (((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) inside = !inside;
        }
        return inside;
    }

    public static float polygonArea(float[] xy) {
        int n = xy.length >> 1;
        float sum = 0f;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            sum += xy[j * 2] * xy[i * 2 + 1] - xy[i * 2] * xy[j * 2 + 1];
        }
        return sum * 0.5f;
    }
}
