package space.nows.util.math.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {
    private static final float[] SQUARE_CCW = {0, 0, 4, 0, 4, 4, 0, 4};
    private static final float[] SQUARE_CW = {0, 0, 0, 4, 4, 4, 4, 0};
    private static final float[] L_SHAPE = {0, 0, 4, 0, 4, 2, 2, 2, 2, 4, 0, 4};

    @Test void intersections() {
        assertTrue(Overlap.circlesOverlap(0, 0, 1, 1.5f, 0, 1));
        assertFalse(Overlap.rectsOverlap(0, 0, 1, 1, 5, 5, 1, 1));
    }

    @Test void segmentIntersection() {
        assertTrue(SegmentIntersection.segmentsIntersect(0, 0, 4, 4, 0, 4, 4, 0));
        assertFalse(SegmentIntersection.segmentsIntersect(0, 0, 4, 0, 0, 1, 4, 1));
        assertTrue(SegmentIntersection.segmentsIntersect(0, 0, 2, 2, 2, 2, 4, 0));
        assertTrue(SegmentIntersection.segmentsIntersect(0, 0, 4, 0, 2, 0, 6, 0));
        assertFalse(SegmentIntersection.segmentsIntersect(0, 0, 1, 0, 2, 0, 3, 0));
        assertTrue(SegmentIntersection.segmentsIntersect(0, 0, 4, 0, 2, 0, 2, 3));
        assertFalse(SegmentIntersection.segmentsIntersect(0, 0, 1, 1, 2, 0, 3, 1));
        assertTrue(SegmentIntersection.segmentsIntersect(1, 1, 1, 1, 0, 0, 2, 2));
        assertFalse(SegmentIntersection.segmentsIntersect(5, 5, 5, 5, 0, 0, 2, 2));
        assertEquals(SegmentIntersection.segmentsIntersect(0, 0, 4, 4, 0, 4, 4, 0), SegmentIntersection.segmentsIntersect(4, 0, 0, 4, 4, 4, 0, 0));
    }

    @Test void polygonQueries() {
        assertTrue(Polygons.pointInPolygon(2, 2, SQUARE_CCW));
        assertFalse(Polygons.pointInPolygon(5, 2, SQUARE_CCW));
        assertTrue(Polygons.pointInPolygon(1, 3, L_SHAPE));
        assertFalse(Polygons.pointInPolygon(3, 3, L_SHAPE));
        assertFalse(Polygons.pointInPolygon(1, 1, new float[0]));
        assertEquals(16f, Polygons.polygonArea(SQUARE_CCW), 1e-6);
        assertEquals(-16f, Polygons.polygonArea(SQUARE_CW), 1e-6);
        assertEquals(12f, Polygons.polygonArea(L_SHAPE), 1e-5);
        assertEquals(0f, Polygons.polygonArea(new float[]{1, 1}), 1e-6);
        assertTrue(Polygons.isCounterClockwise(SQUARE_CCW));
        assertFalse(Polygons.isCounterClockwise(SQUARE_CW));
        assertEquals(16f, Polygons.perimeter(SQUARE_CCW), 1e-5);
        assertEquals(0f, Polygons.perimeter(new float[]{1, 1}), 1e-6);
        assertEquals(8f, Polygons.perimeter(new float[]{0, 0, 4, 0}), 1e-5);
    }

    @Test void convexity() {
        assertTrue(Polygons.isConvex(SQUARE_CCW));
        assertTrue(Polygons.isConvex(SQUARE_CW));
        assertFalse(Polygons.isConvex(L_SHAPE));
        assertTrue(Polygons.isConvex(new float[]{0, 0, 2, 0, 4, 0, 4, 4}));
        assertFalse(Polygons.isConvex(new float[]{0, 0, 1, 1}));
        assertFalse(Polygons.isConvex(new float[0]));
    }

    @Test void polygonsRejectMalformedVertexArrays() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Polygons.polygonArea(new float[]{0, 0, 1}));
        assertTrue(e.getMessage().contains("odd length 3"));
        assertThrows(IllegalArgumentException.class, () -> Polygons.pointInPolygon(0, 0, new float[]{0, 0, 1, 1, 2}));
        assertThrows(IllegalArgumentException.class, () -> Polygons.perimeter(new float[5]));
        assertThrows(IllegalArgumentException.class, () -> Polygons.isConvex(new float[7]));
        assertThrows(NullPointerException.class, () -> Polygons.polygonArea(null));
    }

    @Test void distance() {
        assertEquals(25f, Distance.distanceSq(0, 0, 3, 4), 1e-6);
        assertEquals(5f, Distance.distance(0, 0, 3, 4), 1e-6);
        assertEquals(3f, Distance.distance(0, 0, 0, 1, 2, 2), 1e-6);
        assertEquals(9f, Distance.distanceSq(0, 0, 0, 1, 2, 2), 1e-6);
        assertEquals(7f, Distance.manhattan(0, 0, 3, -4), 1e-6);
        assertEquals(0f, Distance.manhattan(2, 2, 2, 2), 1e-6);
    }
}
