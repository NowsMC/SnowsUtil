package space.nows.util.math.geometry;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GeometryOddTest {
    private static final float[] UNIT_SQUARE_CCW = {0, 0, 1, 0, 1, 1, 0, 1};
    private static final float[] UNIT_SQUARE_CW = {0, 0, 0, 1, 1, 1, 1, 0};
    private static final float[] L_SHAPE = {0, 0, 2, 0, 2, 1, 1, 1, 1, 2, 0, 2};

    @Test
    void distancesAreSymmetricAndConsistent() {
        Random r = new Random(61);
        for (int i = 0; i < 20_000; i++) {
            float x1 = r.nextFloat() * 100 - 50, y1 = r.nextFloat() * 100 - 50;
            float x2 = r.nextFloat() * 100 - 50, y2 = r.nextFloat() * 100 - 50;
            assertEquals(Distance.distance(x1, y1, x2, y2), Distance.distance(x2, y2, x1, y1), 1e-5f);
            assertEquals(0f, Distance.distance(x1, y1, x1, y1));
            assertEquals(Distance.distanceSq(x1, y1, x2, y2), Distance.distance(x1, y1, x2, y2) * Distance.distance(x1, y1, x2, y2), 1e-2f);
            assertTrue(Distance.manhattan(x1, y1, x2, y2) + 1e-4f >= Distance.distance(x1, y1, x2, y2));
        }
        assertEquals(Math.sqrt(3 * 3 + 4 * 4), Distance.distance(0, 0, 3, 4), 1e-6);
    }

    @Test
    void distanceHandlesNonFiniteInputsWithoutCrashing() {
        assertTrue(Float.isNaN(Distance.distance(Float.NaN, 0, 1, 1)));
        assertTrue(Float.isInfinite(Distance.distance(Float.POSITIVE_INFINITY, 0, 0, 0)));
    }

    @Test
    void circleAndRectOverlapAreSymmetricAndIncludeContainment() {
        Random r = new Random(62);
        for (int i = 0; i < 20_000; i++) {
            float x1 = r.nextFloat() * 10, y1 = r.nextFloat() * 10, r1 = r.nextFloat() * 5;
            float x2 = r.nextFloat() * 10, y2 = r.nextFloat() * 10, r2 = r.nextFloat() * 5;
            assertEquals(Overlap.circlesOverlap(x1, y1, r1, x2, y2, r2), Overlap.circlesOverlap(x2, y2, r2, x1, y1, r1));
            float w1 = r.nextFloat() * 5, h1 = r.nextFloat() * 5, w2 = r.nextFloat() * 5, h2 = r.nextFloat() * 5;
            assertEquals(Overlap.rectsOverlap(x1, y1, w1, h1, x2, y2, w2, h2), Overlap.rectsOverlap(x2, y2, w2, h2, x1, y1, w1, h1));
            assertTrue(Overlap.rectsOverlap(x1, y1, w1, h1, x1, y1, w1, h1), "a rectangle overlaps itself");
        }
        assertTrue(Overlap.circlesOverlap(0, 0, 1, 0, 0, 1), "concentric circles overlap");
        assertFalse(Overlap.circlesOverlap(0, 0, 1, 10, 0, 1));
        assertTrue(Overlap.circleRectOverlap(5, 5, 1, 0, 0, 10, 10), "circle inside rect");
        assertFalse(Overlap.circleRectOverlap(100, 100, 1, 0, 0, 10, 10));
    }

    @Test
    void pointInPolygonHandlesInteriorExteriorAndWindingOrder() {
        assertTrue(Polygons.pointInPolygon(0.5f, 0.5f, UNIT_SQUARE_CCW));
        assertTrue(Polygons.pointInPolygon(0.5f, 0.5f, UNIT_SQUARE_CW), "clockwise polygons also contain their interior");
        assertFalse(Polygons.pointInPolygon(1.5f, 0.5f, UNIT_SQUARE_CCW));
        assertFalse(Polygons.pointInPolygon(-0.1f, -0.1f, UNIT_SQUARE_CCW));
        assertTrue(Polygons.pointInPolygon(1.5f, 1.5f, L_SHAPE) == false);
        assertTrue(Polygons.pointInPolygon(0.5f, 1.5f, L_SHAPE));
    }

    @Test
    void areaPerimeterAndWindingOfSquares() {
        assertEquals(1f, Polygons.polygonArea(UNIT_SQUARE_CCW), 1e-6f);
        assertEquals(-1f, Polygons.polygonArea(UNIT_SQUARE_CW), 1e-6f);
        assertEquals(4f, Polygons.perimeter(UNIT_SQUARE_CCW), 1e-6f);
        assertTrue(Polygons.isCounterClockwise(UNIT_SQUARE_CCW));
        assertFalse(Polygons.isCounterClockwise(UNIT_SQUARE_CW));
        assertEquals(3f, Polygons.polygonArea(L_SHAPE), 1e-6f);
    }

    @Test
    void convexityRejectsConcaveAndAcceptsSquare() {
        assertTrue(Polygons.isConvex(UNIT_SQUARE_CCW));
        assertTrue(Polygons.isConvex(UNIT_SQUARE_CW));
        assertFalse(Polygons.isConvex(L_SHAPE));
    }

    @Test
    void collinearPointsAreStillConvex() {
        float[] line = {0, 0, 1, 0, 2, 0, 2, 2, 0, 2};
        assertTrue(Polygons.isConvex(line), "collinear vertices do not make a polygon concave");
    }

    @Test
    void segmentIntersectionCases() {
        assertTrue(SegmentIntersection.segmentsIntersect(0, 0, 2, 2, 0, 2, 2, 0), "crossing diagonals");
        assertFalse(SegmentIntersection.segmentsIntersect(0, 0, 1, 0, 0, 1, 1, 1), "parallel disjoint");
        assertTrue(SegmentIntersection.segmentsIntersect(0, 0, 1, 0, 1, 0, 2, 0), "shared endpoint touches");
        assertTrue(SegmentIntersection.segmentsIntersect(0, 0, 2, 0, 1, 0, 3, 0), "collinear overlap");
        assertFalse(SegmentIntersection.segmentsIntersect(0, 0, 1, 0, 2, 0, 3, 0), "collinear disjoint");
        assertFalse(SegmentIntersection.segmentsIntersect(0, 0, 0, 0, 1, 1, 2, 2), "degenerate point off the other segment");
        assertTrue(SegmentIntersection.segmentsIntersect(1, 1, 1, 1, 0, 0, 2, 2), "degenerate point on the other segment");
    }

    @Test
    void segmentIntersectionIsSymmetricAndOrderIndependent() {
        Random r = new Random(63);
        for (int i = 0; i < 20_000; i++) {
            float ax = r.nextInt(5), ay = r.nextInt(5), bx = r.nextInt(5), by = r.nextInt(5);
            float cx = r.nextInt(5), cy = r.nextInt(5), dx = r.nextInt(5), dy = r.nextInt(5);
            boolean forward = SegmentIntersection.segmentsIntersect(ax, ay, bx, by, cx, cy, dx, dy);
            assertEquals(forward, SegmentIntersection.segmentsIntersect(cx, cy, dx, dy, ax, ay, bx, by), "swap segments");
            assertEquals(forward, SegmentIntersection.segmentsIntersect(bx, by, ax, ay, dx, dy, cx, cy), "reverse both");
        }
    }

    @Test
    void containmentBoundariesAreConsistent() {
        assertTrue(Containment.pointInRect(0.5f, 0.5f, 0, 0, 1, 1));
        assertFalse(Containment.pointInRect(2f, 0.5f, 0, 0, 1, 1));
        assertTrue(Containment.pointInCircle(0, 0, 0, 0, 1));
        assertFalse(Containment.pointInCircle(2, 0, 0, 0, 1));
        assertTrue(Containment.pointInCircle(0, 0, 0, 0, 0), "edges are included, so a zero-radius circle contains its center");
    }
}
