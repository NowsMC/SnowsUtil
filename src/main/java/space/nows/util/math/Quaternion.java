package space.nows.util.math;

public final class Quaternion {
    public float x, y, z, w = 1f;

    public Quaternion() {}

    public Quaternion(float x, float y, float z, float w) { this.x = x; this.y = y; this.z = z; this.w = w; }

    public Quaternion set(float x, float y, float z, float w) { this.x = x; this.y = y; this.z = z; this.w = w; return this; }

    public Quaternion set(Quaternion q) { return set(q.x, q.y, q.z, q.w); }

    public Quaternion identity() { return set(0f, 0f, 0f, 1f); }

    public Quaternion setFromAxisAngle(float ax, float ay, float az, float radians) {
        float half = radians * 0.5f;
        float s = (float) Math.sin(half);
        return set(ax * s, ay * s, az * s, (float) Math.cos(half));
    }

    public float lengthSq() { return x * x + y * y + z * z + w * w; }

    public Quaternion normalize() {
        float len = (float) Math.sqrt(lengthSq());
        if (len != 0f) { float inv = 1f / len; x *= inv; y *= inv; z *= inv; w *= inv; }
        return this;
    }

    public Quaternion conjugate() { x = -x; y = -y; z = -z; return this; }

    public Quaternion multiply(Quaternion o) {
        float nx = w * o.x + x * o.w + y * o.z - z * o.y;
        float ny = w * o.y - x * o.z + y * o.w + z * o.x;
        float nz = w * o.z + x * o.y - y * o.x + z * o.w;
        float nw = w * o.w - x * o.x - y * o.y - z * o.z;
        return set(nx, ny, nz, nw);
    }

    public float dot(Quaternion o) { return x * o.x + y * o.y + z * o.z + w * o.w; }

    public Vec3 rotate(Vec3 v) {

        float tx = 2f * (y * v.z - z * v.y);
        float ty = 2f * (z * v.x - x * v.z);
        float tz = 2f * (x * v.y - y * v.x);
        v.x += w * tx + (y * tz - z * ty);
        v.y += w * ty + (z * tx - x * tz);
        v.z += w * tz + (x * ty - y * tx);
        return v;
    }

    public Quaternion slerp(Quaternion target, float t) {
        float cos = dot(target);
        float tx = target.x, ty = target.y, tz = target.z, tw = target.w;
        if (cos < 0f) { cos = -cos; tx = -tx; ty = -ty; tz = -tz; tw = -tw; }
        float k0, k1;
        if (cos > 0.9995f) {
            k0 = 1f - t;
            k1 = t;
        } else {
            float theta = (float) Math.acos(cos);
            float sin = (float) Math.sin(theta);
            k0 = (float) Math.sin((1f - t) * theta) / sin;
            k1 = (float) Math.sin(t * theta) / sin;
        }
        return set(k0 * x + k1 * tx, k0 * y + k1 * ty, k0 * z + k1 * tz, k0 * w + k1 * tw).normalize();
    }

    public Quaternion copy() { return new Quaternion(x, y, z, w); }

    @Override public String toString() { return "Quaternion(" + x + ", " + y + ", " + z + ", " + w + ")"; }
}
