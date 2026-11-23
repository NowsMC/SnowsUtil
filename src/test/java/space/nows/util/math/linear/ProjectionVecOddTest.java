package space.nows.util.math.linear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectionVecOddTest {
    @Test
    void orthoMapsViewVolumeCornersToNdcCube() {
        Mat4 o = Mat4Projection.ortho(new Mat4(), -1f, 1f, -1f, 1f, 1f, 10f);
        Vec3 nearCorner = o.transformPoint(new Vec3(1f, 1f, -1f));
        assertEquals(1f, nearCorner.x, 1e-5f);
        assertEquals(1f, nearCorner.y, 1e-5f);
        assertEquals(-1f, nearCorner.z, 1e-5f);
        Vec3 farCorner = o.transformPoint(new Vec3(-1f, -1f, -10f));
        assertEquals(-1f, farCorner.x, 1e-5f);
        assertEquals(1f, farCorner.z, 1e-4f);
    }

    @Test
    void perspectiveMapsNearAndFarPlanesAndKeepsCenterOnAxis() {
        Mat4 p = Mat4Projection.perspective(new Mat4(), (float) (Math.PI / 2), 1f, 1f, 10f);
        Vec3 near = p.transformPoint(new Vec3(1f, 1f, -1f));
        assertEquals(1f, near.x, 1e-5f);
        assertEquals(-1f, near.z, 1e-5f);
        Vec3 far = p.transformPoint(new Vec3(10f, 10f, -10f));
        assertEquals(1f, far.z, 1e-5f);
        Vec3 center = p.transformPoint(new Vec3(0f, 0f, -5f));
        assertEquals(0f, center.x, 1e-6f);
        assertEquals(7f / 9f, center.z, 1e-5f);
    }

    @Test
    void perspectiveRejectsDegenerateParameters() {
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(new Mat4(), 0f, 1f, 1f, 10f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(new Mat4(), 1f, 1f, 0f, 10f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(new Mat4(), 1f, 1f, 10f, 1f));
    }

    @Test
    void zeroVectorNormalizesToZeroNotNaN() {
        Vec2 v = new Vec2(0f, 0f).normalize();
        assertEquals(0f, v.x);
        assertEquals(0f, v.y);
        Vec3 w = new Vec3(0f, 0f, 0f);
        assertFalse(Float.isNaN(w.normalize().x), "Vec3 normalize of zero must not produce NaN");
    }

    @Test
    void crossAndDotBehaveAsExpected() {
        assertEquals(1f, new Vec2(1f, 0f).cross(new Vec2(0f, 1f)));
        assertEquals(-1f, new Vec2(0f, 1f).cross(new Vec2(1f, 0f)));
        assertEquals(0f, new Vec2(2f, 0f).dot(new Vec2(0f, 5f)));
    }

    @Test
    void rotateByQuarterTurnMovesXToY() {
        Vec2 v = new Vec2(1f, 0f).rotate((float) (Math.PI / 2));
        assertEquals(0f, v.x, 1e-6f);
        assertEquals(1f, v.y, 1e-6f);
    }
}
