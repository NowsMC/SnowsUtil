package space.nows.util.math.geometry;

/** Point-in-polygon and signed area for flat {@code x,y} vertex arrays. */
public final class Polygons {
    private Polygons() {}

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
