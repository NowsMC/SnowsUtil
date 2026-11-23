package space.nows.util.math.linear;

import org.junit.jupiter.api.Test;
import space.nows.util.error.SingularMatrixException;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class LinearOddTest {
    private static void assertMatrixNear(Mat4 expected, Mat4 actual, float eps) {
        for (int i = 0; i < 16; i++) {
            assertEquals(expected.m[i], actual.m[i], eps, "entry " + i);
        }
    }

    private static Mat4 randomAffine(Random r) {
        Quaternion q = new Quaternion().setFromAxisAngle(r.nextFloat(), r.nextFloat(), r.nextFloat() + 0.1f, r.nextFloat() * 6f);
        Mat4 rot = new Mat4().setRotation(q);
        Mat4 scale = new Mat4().setScale(0.5f + r.nextFloat() * 3, 0.5f + r.nextFloat() * 3, 0.5f + r.nextFloat() * 3);
        Mat4 trans = new Mat4().setTranslation(r.nextFloat() * 10 - 5, r.nextFloat() * 10 - 5, r.nextFloat() * 10 - 5);
        return trans.multiply(rot).multiply(scale);
    }

    @Test
    void inverseOfIdentityIsIdentity() {
        assertMatrixNear(new Mat4(), Mat4Inverse.invert(new Mat4()), 1e-6f);
    }

    @Test
    void inverseTimesMatrixIsIdentityForRandomAffineMatrices() {
        Random r = new Random(71);
        for (int i = 0; i < 2000; i++) {
            Mat4 m = randomAffine(r);
            Mat4 product = Mat4Inverse.invert(m.copy()).multiply(m.copy());
            assertMatrixNear(new Mat4(), product, 1e-3f);
        }
    }

    @Test
    void singularMatricesAreRejected() {
        Mat4 zeroRow = new Mat4();
        for (int c = 0; c < 4; c++) zeroRow.set(1, c, 0f);
        assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(zeroRow));
        Mat4 nan = new Mat4();
        nan.set(0, 0, Float.NaN);
        assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(nan));
        Mat4 duplicateRows = new Mat4();
        for (int c = 0; c < 4; c++) duplicateRows.set(1, c, duplicateRows.get(0, c));
        assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(duplicateRows));
    }

    @Test
    void transformPointRoundTripsThroughInverse() {
        Random r = new Random(72);
        for (int i = 0; i < 500; i++) {
            Mat4 m = randomAffine(r);
            Mat4 inv = Mat4Inverse.invert(m.copy());
            Vec3 p = new Vec3(r.nextFloat() * 4 - 2, r.nextFloat() * 4 - 2, r.nextFloat() * 4 - 2);
            Vec3 back = inv.transformPoint(m.transformPoint(new Vec3(p)));
            assertEquals(p.x, back.x, 1e-3f);
            assertEquals(p.y, back.y, 1e-3f);
            assertEquals(p.z, back.z, 1e-3f);
        }
    }

    @Test
    void quaternionInverseCancelsRotation() {
        Random r = new Random(73);
        for (int i = 0; i < 1000; i++) {
            Quaternion q = new Quaternion().setFromAxisAngle(r.nextFloat() + 0.1f, r.nextFloat(), r.nextFloat(), r.nextFloat() * 6);
            Quaternion product = q.copy().multiply(q.copy().invert());
            assertEquals(0f, product.x, 1e-5f);
            assertEquals(0f, product.y, 1e-5f);
            assertEquals(0f, product.z, 1e-5f);
            assertEquals(1f, product.w, 1e-5f);
        }
    }

    @Test
    void quaternionZeroLengthOperationsAreRejectedOrObviouslyDefined() {
        Quaternion zero = new Quaternion(0f, 0f, 0f, 0f);
        assertThrows(SingularMatrixException.class, zero::invert);
        Quaternion norm = new Quaternion(0f, 0f, 0f, 0f).normalize();
        assertFalse(Float.isNaN(norm.w), "normalizing a zero quaternion must not silently produce NaN");
    }

    @Test
    void slerpHitsEndpointsAndStaysUnitLength() {
        Random r = new Random(74);
        for (int i = 0; i < 2000; i++) {
            Quaternion a = new Quaternion().setFromAxisAngle(r.nextFloat() + 0.1f, r.nextFloat(), r.nextFloat(), r.nextFloat() * 6);
            Quaternion b = new Quaternion().setFromAxisAngle(r.nextFloat() + 0.1f, r.nextFloat(), r.nextFloat(), r.nextFloat() * 6);
            Quaternion start = a.copy().slerp(b, 0f);
            Quaternion end = a.copy().slerp(b, 1f);
            assertEquals(1f, start.length(), 1e-4f);
            float sameStart = Math.abs(start.dot(a));
            float sameEnd = Math.abs(end.dot(b));
            assertEquals(1f, sameStart, 1e-3f, "slerp(0) must be a");
            assertEquals(1f, sameEnd, 1e-3f, "slerp(1) must be b");
            assertEquals(1f, a.copy().slerp(b, 0.37f).length(), 1e-4f);
        }
    }

    @Test
    void slerpBetweenAntipodalQuaternionsStaysValid() {
        Quaternion a = new Quaternion(0f, 0f, 0f, 1f);
        Quaternion minusA = new Quaternion(0f, 0f, 0f, -1f);
        Quaternion mid = a.copy().slerp(minusA, 0.5f);
        assertFalse(Float.isNaN(mid.w), "antipodal slerp must not produce NaN");
        assertEquals(1f, mid.length(), 1e-4f);
    }

    @Test
    void rotateByIdentityAndNinetyDegrees() {
        Vec3 unchanged = new Quaternion().identity().rotate(new Vec3(1, 2, 3));
        assertEquals(1f, unchanged.x, 1e-6f);
        assertEquals(2f, unchanged.y, 1e-6f);
        assertEquals(3f, unchanged.z, 1e-6f);
        Quaternion quarterTurnZ = new Quaternion().setFromAxisAngle(0, 0, 1, (float) (Math.PI / 2));
        Vec3 rotated = quarterTurnZ.rotate(new Vec3(1, 0, 0));
        assertEquals(0f, rotated.x, 1e-6f);
        assertEquals(1f, rotated.y, 1e-6f);
    }

    @Test
    void quaternionEqualityFollowsFloatCompare() {
        assertEquals(new Quaternion(0, 0, 0, 1), new Quaternion(0, 0, 0, 1));
        assertEquals(new Quaternion(0, 0, 0, 1).hashCode(), new Quaternion(0, 0, 0, 1).hashCode());
        assertNotEquals(new Quaternion(0, 0, 0, 1), new Quaternion(0, 0, 0, Float.NaN));
    }
}
