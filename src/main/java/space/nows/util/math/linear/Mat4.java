package space.nows.util.math.linear;

import space.nows.util.check.Preconditions;

import java.util.Arrays;

/**
 * 4x4 float matrix in column-major order ({@code m[col * 4 + row]}). Mutating methods return {@code this}.
 * Projection matrices live in {@link Mat4Projection}; inversion lives in {@link Mat4Inverse}.
 */
public final class Mat4 {
    public final float[] m = new float[16];

    public Mat4() { identity(); }

    public Mat4 identity() {
        Arrays.fill(m, 0f);
        m[0] = m[5] = m[10] = m[15] = 1f;
        return this;
    }

    public Mat4 set(Mat4 o) { System.arraycopy(o.m, 0, m, 0, 16); return this; }

    /** @throws IndexOutOfBoundsException unless {@code row} and {@code col} are in {@code [0, 4)} */
    public float get(int row, int col) { return m[Preconditions.index(col, 4) * 4 + Preconditions.index(row, 4)]; }

    public Mat4 set(int row, int col, float value) {
        m[Preconditions.index(col, 4) * 4 + Preconditions.index(row, 4)] = value;
        return this;
    }

    public Mat4 setTranslation(float tx, float ty, float tz) {
        identity();
        m[12] = tx; m[13] = ty; m[14] = tz;
        return this;
    }

    public Mat4 setScale(float sx, float sy, float sz) {
        identity();
        m[0] = sx; m[5] = sy; m[10] = sz;
        return this;
    }

    public Mat4 setRotation(Quaternion q) {
        float xx = q.x * q.x, yy = q.y * q.y, zz = q.z * q.z;
        float xy = q.x * q.y, xz = q.x * q.z, yz = q.y * q.z;
        float wx = q.w * q.x, wy = q.w * q.y, wz = q.w * q.z;
        identity();
        m[0] = 1f - 2f * (yy + zz);
        m[1] = 2f * (xy + wz);
        m[2] = 2f * (xz - wy);
        m[4] = 2f * (xy - wz);
        m[5] = 1f - 2f * (xx + zz);
        m[6] = 2f * (yz + wx);
        m[8] = 2f * (xz + wy);
        m[9] = 2f * (yz - wx);
        m[10] = 1f - 2f * (xx + yy);
        return this;
    }

    public Mat4 multiply(Mat4 o) { multiply(this, o, this); return this; }

    public static void multiply(Mat4 a, Mat4 b, Mat4 out) {
        float[] x = a.m, y = b.m;
        float a00 = x[0], a01 = x[1], a02 = x[2], a03 = x[3];
        float a10 = x[4], a11 = x[5], a12 = x[6], a13 = x[7];
        float a20 = x[8], a21 = x[9], a22 = x[10], a23 = x[11];
        float a30 = x[12], a31 = x[13], a32 = x[14], a33 = x[15];
        float[] r = out.m;
        for (int c = 0; c < 4; c++) {
            float b0 = y[c * 4], b1 = y[c * 4 + 1], b2 = y[c * 4 + 2], b3 = y[c * 4 + 3];
            r[c * 4]     = a00 * b0 + a10 * b1 + a20 * b2 + a30 * b3;
            r[c * 4 + 1] = a01 * b0 + a11 * b1 + a21 * b2 + a31 * b3;
            r[c * 4 + 2] = a02 * b0 + a12 * b1 + a22 * b2 + a32 * b3;
            r[c * 4 + 3] = a03 * b0 + a13 * b1 + a23 * b2 + a33 * b3;
        }
    }

    public Mat4 transpose() {
        float t;
        t = m[1];  m[1]  = m[4];  m[4]  = t;
        t = m[2];  m[2]  = m[8];  m[8]  = t;
        t = m[3];  m[3]  = m[12]; m[12] = t;
        t = m[6];  m[6]  = m[9];  m[9]  = t;
        t = m[7];  m[7]  = m[13]; m[13] = t;
        t = m[11]; m[11] = m[14]; m[14] = t;
        return this;
    }

    public Vec3 transformPoint(Vec3 v) {
        float x = v.x, y = v.y, z = v.z;
        float nx = m[0] * x + m[4] * y + m[8] * z + m[12];
        float ny = m[1] * x + m[5] * y + m[9] * z + m[13];
        float nz = m[2] * x + m[6] * y + m[10] * z + m[14];
        float nw = m[3] * x + m[7] * y + m[11] * z + m[15];
        if (nw != 1f && nw != 0f) { float inv = 1f / nw; nx *= inv; ny *= inv; nz *= inv; }
        return v.set(nx, ny, nz);
    }

    public Vec3 transformDirection(Vec3 v) {
        float x = v.x, y = v.y, z = v.z;
        return v.set(m[0] * x + m[4] * y + m[8] * z,
                     m[1] * x + m[5] * y + m[9] * z,
                     m[2] * x + m[6] * y + m[10] * z);
    }

    public float determinant() {
        float a = m[0], b = m[4], c = m[8], d = m[12];
        float e = m[1], f = m[5], g = m[9], h = m[13];
        float i = m[2], j = m[6], k = m[10], l = m[14];
        float n = m[3], o = m[7], p = m[11], q = m[15];
        float kq_lp = k * q - l * p, jq_lo = j * q - l * o, jp_ko = j * p - k * o;
        float iq_ln = i * q - l * n, ip_kn = i * p - k * n, io_jn = i * o - j * n;
        return a * (f * kq_lp - g * jq_lo + h * jp_ko)
             - b * (e * kq_lp - g * iq_ln + h * ip_kn)
             + c * (e * jq_lo - f * iq_ln + h * io_jn)
             - d * (e * jp_ko - f * ip_kn + g * io_jn);
    }

    public Mat4 copy() { return new Mat4().set(this); }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("Mat4[");
        for (int r = 0; r < 4; r++) {
            if (r > 0) sb.append("; ");
            for (int c = 0; c < 4; c++) {
                if (c > 0) sb.append(", ");
                sb.append(get(r, c));
            }
        }
        return sb.append(']').toString();
    }

    @Override public boolean equals(Object o) { return o instanceof Mat4 other && Arrays.equals(m, other.m); }

    @Override public int hashCode() { return Arrays.hashCode(m); }
}
