package space.nows.util.math.linear;

import org.junit.jupiter.api.Test;
import space.nows.util.math.FastMath;

import static org.junit.jupiter.api.Assertions.*;

class LinearTest {
    @Test void vectorsAndMatrices() {
        Vec3 v = new Vec3(1, 0, 0).cross(new Vec3(0, 1, 0));
        assertEquals(new Vec3(0, 0, 1), v);

        Quaternion q = new Quaternion().setFromAxisAngle(0, 0, 1, FastMath.HALF_PI);
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
}
