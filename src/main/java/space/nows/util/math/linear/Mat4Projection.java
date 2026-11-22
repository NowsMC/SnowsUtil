package space.nows.util.math.linear;

import space.nows.util.math.Angles;

import java.util.Arrays;

/** Builds perspective and orthographic projection matrices into an existing {@link Mat4}. */
public final class Mat4Projection {
    private Mat4Projection() {}

    /** Writes a perspective projection into {@code target} and returns it. */
    public static Mat4 perspective(Mat4 target, float fovYRadians, float aspect, float near, float far) {
        if (!(fovYRadians > 0f && fovYRadians < Angles.PI)) {
            throw new IllegalArgumentException("fovYRadians must be in (0, PI) but was " + fovYRadians);
        }
        if (!(aspect > 0f && Float.isFinite(aspect))) throw new IllegalArgumentException("aspect must be finite and > 0 but was " + aspect);
        if (!(near > 0f && far > near && Float.isFinite(far))) {
            throw new IllegalArgumentException("need 0 < near < far (finite) but was near=" + near + ", far=" + far);
        }
        float[] m = target.m;
        float f = 1f / (float) Math.tan(fovYRadians * 0.5f);
        Arrays.fill(m, 0f);
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (far + near) / (near - far);
        m[11] = -1f;
        m[14] = (2f * far * near) / (near - far);
        return target;
    }

    /** Writes an orthographic projection into {@code target} and returns it. */
    public static Mat4 ortho(Mat4 target, float left, float right, float bottom, float top, float near, float far) {
        if (!(left != right && bottom != top && near != far)
                || !Float.isFinite(right - left) || !Float.isFinite(top - bottom) || !Float.isFinite(far - near)) {
            throw new IllegalArgumentException("ortho volume must have finite, non-zero extent on every axis");
        }
        target.identity();
        float[] m = target.m;
        m[0] = 2f / (right - left);
        m[5] = 2f / (top - bottom);
        m[10] = -2f / (far - near);
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(far + near) / (far - near);
        return target;
    }
}
