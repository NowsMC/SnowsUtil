package space.nows.util.math.linear;

import org.junit.jupiter.api.Test;
import space.nows.util.error.SingularMatrixException;
import space.nows.util.math.Angles;
import space.nows.util.math.FastRandom;

import static org.junit.jupiter.api.Assertions.*;

class LinearTest {
    private static void assertIdentity(Mat4 m, double eps) {
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) assertEquals(r == c ? 1.0 : 0.0, m.get(r, c), eps, "m[" + r + "][" + c + "]");
    }

    @Test void vectorsAndMatrices() {
        Vec3 v = new Vec3(1, 0, 0).cross(new Vec3(0, 1, 0));
        assertEquals(new Vec3(0, 0, 1), v);

        Quaternion q = new Quaternion().setFromAxisAngle(0, 0, 1, Angles.HALF_PI);
        Vec3 r = q.rotate(new Vec3(1, 0, 0));
        assertEquals(0f, r.x, 1e-5);
        assertEquals(1f, r.y, 1e-5);

        Mat4 t = new Mat4().setTranslation(1, 2, 3);
        Vec3 p = t.transformPoint(new Vec3(1, 1, 1));
        assertEquals(new Vec3(2, 3, 4), p);
        assertEquals(1f, new Mat4().determinant(), 1e-6);

        Mat4 rot = new Mat4().setRotation(q);
        Vec3 d = rot.transformDirection(new Vec3(1, 0, 0));
        assertEquals(1f, d.y, 1e-5);
    }

    // ---------------------------------------------------------------- Mat4
    @Test void inverseTimesOriginalIsIdentityForRandomTransforms() {
        FastRandom rnd = new FastRandom(61);
        for (int i = 0; i < 500; i++) {
            Quaternion q = new Quaternion(rnd.nextFloat(-1f, 1f), rnd.nextFloat(-1f, 1f), rnd.nextFloat(-1f, 1f), rnd.nextFloat(0.1f, 1f)).normalize();
            Mat4 m = new Mat4().setRotation(q);
            m.multiply(new Mat4().setScale(rnd.nextFloat(0.5f, 3f), rnd.nextFloat(0.5f, 3f), rnd.nextFloat(0.5f, 3f)));
            m.multiply(new Mat4().setTranslation(rnd.nextFloat(-50f, 50f), rnd.nextFloat(-50f, 50f), rnd.nextFloat(-50f, 50f)));
            Mat4 inv = Mat4Inverse.invert(m.copy());
            Mat4 prod = new Mat4();
            Mat4.multiply(m, inv, prod);
            assertIdentity(prod, 1e-3);
            Mat4.multiply(inv, m, prod);
            assertIdentity(prod, 1e-3);

            Vec3 p = new Vec3(rnd.nextFloat(-10f, 10f), rnd.nextFloat(-10f, 10f), rnd.nextFloat(-10f, 10f));
            Vec3 back = inv.transformPoint(m.transformPoint(p));
            assertEquals(p.x, back.x, 1e-2);
            assertEquals(p.y, back.y, 1e-2);
            assertEquals(p.z, back.z, 1e-2);
        }
    }

    @Test void inverseNeedsPivotingToHandleAZeroOnTheDiagonal() {
        Mat4 swap = new Mat4();
        swap.set(0, 0, 0f).set(1, 1, 0f).set(0, 1, 1f).set(1, 0, 1f);   // swaps x and y; m[0][0] == 0
        Mat4 inv = Mat4Inverse.invert(swap.copy());
        Mat4 prod = new Mat4();
        Mat4.multiply(swap, inv, prod);
        assertIdentity(prod, 1e-6);
    }

    @Test void invertingAPerspectiveMatrixRoundTrips() {
        Mat4 proj = Mat4Projection.perspective(new Mat4(), 1.0f, 16f / 9f, 0.1f, 100f);
        Mat4 inv = Mat4Inverse.invert(proj.copy());
        Mat4 prod = new Mat4();
        Mat4.multiply(proj, inv, prod);
        assertIdentity(prod, 1e-3);
    }

    @Test void singularMatricesAreRejectedAndLeftUntouched() {
        Mat4 flat = new Mat4().setScale(1f, 0f, 1f);
        Mat4 before = flat.copy();
        SingularMatrixException e = assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(flat));
        assertTrue(e.getMessage().contains("singular"));
        assertEquals(before, flat);
        Mat4 zero = new Mat4().setScale(0f, 0f, 0f);
        assertThrows(ArithmeticException.class, () -> Mat4Inverse.invert(zero));
        Mat4 nan = new Mat4().set(0, 0, Float.NaN);
        assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(nan));
    }

    @Test void elementAccessIsBoundsChecked() {
        Mat4 m = new Mat4();
        m.set(2, 3, 9f);
        assertEquals(9f, m.get(2, 3));
        assertEquals(1f, m.get(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(4, 0));      // used to silently return m[4]
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> m.set(4, 4, 1f));
    }

    @Test void projectionsRejectParametersThatWouldProduceNaNOrInfinity() {
        Mat4 m = new Mat4();
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(m, 0f, 1f, 0.1f, 10f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(m, (float) Math.PI, 1f, 0.1f, 10f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(m, Float.NaN, 1f, 0.1f, 10f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(m, 1f, 0f, 0.1f, 10f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(m, 1f, 1f, 0f, 10f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(m, 1f, 1f, 5f, 5f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(m, 1f, 1f, 5f, 1f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.perspective(m, 1f, 1f, 0.1f, Float.POSITIVE_INFINITY));
        assertDoesNotThrow(() -> Mat4Projection.perspective(m, 1f, 1f, 0.1f, 10f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.ortho(m, 1f, 1f, 0f, 1f, 0f, 1f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.ortho(m, 0f, 1f, 2f, 2f, 0f, 1f));
        assertThrows(IllegalArgumentException.class, () -> Mat4Projection.ortho(m, 0f, 1f, 0f, 1f, 3f, 3f));
        assertDoesNotThrow(() -> Mat4Projection.ortho(m, 0f, 800f, 0f, 600f, -1f, 1f));
    }

    @Test void matrixEqualityCopyAndHash() {
        Mat4 a = new Mat4().setTranslation(1, 2, 3);
        Mat4 b = a.copy();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotSame(a, b);
        b.set(0, 0, 2f);
        assertNotEquals(a, b);
        assertEquals(1f, a.get(0, 0));                                        // the copy is independent
        assertNotEquals(a, null);
        assertNotEquals(a, "x");
    }

    // ---------------------------------------------------------------- Quaternion
    @Test void quaternionInverse() {
        Quaternion q = new Quaternion(1f, 2f, 3f, 4f);            // deliberately not unit length
        Quaternion inv = q.copy().invert();
        Quaternion prod = q.copy().multiply(inv);
        assertEquals(0f, prod.x, 1e-6);
        assertEquals(0f, prod.y, 1e-6);
        assertEquals(0f, prod.z, 1e-6);
        assertEquals(1f, prod.w, 1e-6);
        assertEquals((float) Math.sqrt(30), q.length(), 1e-5);
        SingularMatrixException e = assertThrows(SingularMatrixException.class, () -> new Quaternion(0f, 0f, 0f, 0f).invert());
        assertTrue(e.getMessage().contains("quaternion"));
        assertThrows(SingularMatrixException.class, () -> new Quaternion(Float.NaN, 0f, 0f, 1f).invert());
    }

    @Test void quaternionEquality() {
        Quaternion a = new Quaternion(1f, 2f, 3f, 4f), b = new Quaternion(1f, 2f, 3f, 4f);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new Quaternion(1f, 2f, 3f, 5f));
        assertNotEquals(a, null);
        assertEquals(new Quaternion(), new Quaternion().identity());
    }

    @Test void slerpEndpointsAndHalfway() {
        Quaternion a = new Quaternion(), b = new Quaternion().setFromAxisAngle(0, 0, 1, Angles.HALF_PI);
        Quaternion start = a.copy().slerp(b, 0f), end = a.copy().slerp(b, 1f), mid = a.copy().slerp(b, 0.5f);
        assertEquals(1f, start.dot(a), 1e-5);
        assertEquals(1f, Math.abs(end.dot(b)), 1e-5);
        Quaternion eighth = new Quaternion().setFromAxisAngle(0, 0, 1, Angles.HALF_PI / 2f);
        assertEquals(1f, Math.abs(mid.dot(eighth)), 1e-4);
    }
}
