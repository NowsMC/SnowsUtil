package space.nows.util.math.linear;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RemainingLinearTest {
    @Test
    void transposeIsAnInvolutionAndSwapsOffDiagonals() {
        Random r = new Random(151);
        for (int i = 0; i < 1000; i++) {
            Mat4 m = new Mat4();
            for (int k = 0; k < 16; k++) m.m[k] = r.nextFloat() * 10 - 5;
            Mat4 t = m.copy().transpose();
            for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) assertEquals(m.get(row, col), t.get(col, row));
            assertEquals(m, t.copy().transpose());
        }
    }

    @Test
    void determinantOfKnownMatrices() {
        assertEquals(1f, new Mat4().determinant(), 1e-6f);
        Mat4 scale = new Mat4().setScale(2f, 3f, 4f);
        assertEquals(24f, scale.determinant(), 1e-4f);
        Mat4 singular = new Mat4();
        for (int c = 0; c < 4; c++) singular.set(3, c, 0f);
        assertEquals(0f, singular.determinant(), 1e-6f);
        Mat4 swap = new Mat4().identity();
        swap.set(0, 0, 0f).set(1, 1, 0f).set(0, 1, 1f).set(1, 0, 1f);
        assertEquals(-1f, swap.determinant(), 1e-6f);
    }

    @Test
    void determinantIsMultiplicative() {
        Random r = new Random(152);
        for (int i = 0; i < 500; i++) {
            Mat4 a = new Mat4(), b = new Mat4();
            for (int k = 0; k < 16; k++) { a.m[k] = r.nextFloat() * 4 - 2; b.m[k] = r.nextFloat() * 4 - 2; }
            Mat4 ab = a.copy().multiply(b.copy());
            float expected = a.determinant() * b.determinant();
            assertEquals(expected, ab.determinant(), 1e-2f * Math.max(1f, Math.abs(expected)));
        }
    }

    @Test
    void setRotationMatchesQuaternionRotation() {
        Random r = new Random(153);
        for (int i = 0; i < 500; i++) {
            Quaternion q = new Quaternion().setFromAxisAngle(r.nextFloat() + 0.1f, r.nextFloat(), r.nextFloat(), r.nextFloat() * 6);
            Mat4 m = new Mat4().setRotation(q);
            Vec3 v = new Vec3(r.nextFloat() * 2 - 1, r.nextFloat() * 2 - 1, r.nextFloat() * 2 - 1);
            Vec3 viaMatrix = m.transformDirection(new Vec3(v));
            Vec3 viaQuaternion = q.copy().rotate(new Vec3(v));
            assertEquals(viaQuaternion.x, viaMatrix.x, 1e-4f);
            assertEquals(viaQuaternion.y, viaMatrix.y, 1e-4f);
            assertEquals(viaQuaternion.z, viaMatrix.z, 1e-4f);
        }
    }

    @Test
    void quarterTurnAboutZMapsXToY() {
        Mat4 m = new Mat4().setRotation(new Quaternion().setFromAxisAngle(0f, 0f, 1f, (float) (Math.PI / 2)));
        Vec3 v = m.transformDirection(new Vec3(1f, 0f, 0f));
        assertEquals(0f, v.x, 1e-6f);
        assertEquals(1f, v.y, 1e-6f);
    }
}
