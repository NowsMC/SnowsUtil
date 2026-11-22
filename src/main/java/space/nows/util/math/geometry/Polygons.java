package space.nows.util.math.geometry;

import space.nows.util.error.Checks;

/** Point-in-polygon, area, winding and convexity for flat {@code x0,y0,x1,y1,...} vertex arrays (simple polygons). */
public final class Polygons {
    private Polygons() {}

    private static int vertexCount(float[] xy) {
        Checks.notNull(xy, "xy");
        if ((xy.length & 1) != 0) {
            throw new IllegalArgumentException("vertex array must hold x,y pairs but has odd length " + xy.length);
        }
        return xy.length >> 1;
    }

    public static boolean pointInPolygon(float px, float py, float[] xy) {
        int n = vertexCount(xy);
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            float xi = xy[i * 2], yi = xy[i * 2 + 1], xj = xy[j * 2], yj = xy[j * 2 + 1];
            if (((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) inside = !inside;
        }
        return inside;
    }

    /** Signed area: positive for counter-clockwise vertex order, negative for clockwise. */
    public static float polygonArea(float[] xy) {
        int n = vertexCount(xy);
        float sum = 0f;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            sum += xy[j * 2] * xy[i * 2 + 1] - xy[i * 2] * xy[j * 2 + 1];
        }
        return sum * 0.5f;
    }

    public static boolean isCounterClockwise(float[] xy) { return polygonArea(xy) > 0f; }

    public static float perimeter(float[] xy) {
        int n = vertexCount(xy);
        float sum = 0f;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            sum += Distance.distance(xy[j * 2], xy[j * 2 + 1], xy[i * 2], xy[i * 2 + 1]);
        }
        return n < 2 ? 0f : sum;
    }

    /** True if every turn goes the same way (collinear vertices allowed). Needs at least 3 vertices. Assumes a simple polygon. */
    public static boolean isConvex(float[] xy) {
        int n = vertexCount(xy);
        if (n < 3) return false;
        boolean pos = false, neg = false;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n, k = (i + 2) % n;
            float cross = (xy[j * 2] - xy[i * 2]) * (xy[k * 2 + 1] - xy[j * 2 + 1])
                        - (xy[j * 2 + 1] - xy[i * 2 + 1]) * (xy[k * 2] - xy[j * 2]);
            if (cross > 0f) pos = true; else if (cross < 0f) neg = true;
            if (pos && neg) return false;
        }
        return true;
    }
}
