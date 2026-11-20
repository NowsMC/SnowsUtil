package space.nows.util.math.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {
    @Test void intersections() {
        assertTrue(Intersections.circlesOverlap(0, 0, 1, 1.5f, 0, 1));
        assertFalse(Intersections.rectsOverlap(0, 0, 1, 1, 5, 5, 1, 1));
    }

    @Test void polygons() {
        float[] square = {0, 0, 4, 0, 4, 4, 0, 4};
        assertTrue(Polygons.pointInPolygon(2, 2, square));
        assertFalse(Polygons.pointInPolygon(5, 2, square));
        assertEquals(16f, Polygons.polygonArea(square), 1e-6);
    }

    @Test void distance() {
        assertEquals(25f, Distance.distanceSq(0, 0, 3, 4), 1e-6);
        assertEquals(5f, Distance.distance(0, 0, 3, 4), 1e-6);
    }
}
