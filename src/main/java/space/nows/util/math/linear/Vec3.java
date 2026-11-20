package space.nows.util.math.linear;

public final class Vec3 {
    public float x, y, z;

    public Vec3() {}

    public Vec3(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }

    public Vec3(Vec3 o) { this.x = o.x; this.y = o.y; this.z = o.z; }

    public Vec3 set(float x, float y, float z) { this.x = x; this.y = y; this.z = z; return this; }

    public Vec3 set(Vec3 o) { this.x = o.x; this.y = o.y; this.z = o.z; return this; }

    public Vec3 add(float ox, float oy, float oz) { x += ox; y += oy; z += oz; return this; }

    public Vec3 add(Vec3 o) { x += o.x; y += o.y; z += o.z; return this; }

    public Vec3 sub(float ox, float oy, float oz) { x -= ox; y -= oy; z -= oz; return this; }

    public Vec3 sub(Vec3 o) { x -= o.x; y -= o.y; z -= o.z; return this; }

    public Vec3 scale(float s) { x *= s; y *= s; z *= s; return this; }

    public Vec3 negate() { x = -x; y = -y; z = -z; return this; }

    public float dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }

    public Vec3 cross(Vec3 o) {
        float nx = y * o.z - z * o.y;
        float ny = z * o.x - x * o.z;
        float nz = x * o.y - y * o.x;
        x = nx; y = ny; z = nz;
        return this;
    }

    public float lengthSq() { return x * x + y * y + z * z; }

    public float length() { return (float) Math.sqrt(x * x + y * y + z * z); }

    public float distanceSq(Vec3 o) {
        float dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public float distance(Vec3 o) { return (float) Math.sqrt(distanceSq(o)); }

    public Vec3 normalize() {
        float len = length();
        if (len != 0f) { float inv = 1f / len; x *= inv; y *= inv; z *= inv; }
        return this;
    }

    public Vec3 lerp(Vec3 target, float t) {
        x += (target.x - x) * t;
        y += (target.y - y) * t;
        z += (target.z - z) * t;
        return this;
    }

    public Vec3 copy() { return new Vec3(x, y, z); }

    @Override public boolean equals(Object o) {
        return o instanceof Vec3 v && Float.compare(x, v.x) == 0 && Float.compare(y, v.y) == 0 && Float.compare(z, v.z) == 0;
    }

    @Override public int hashCode() { return 31 * (31 * Float.hashCode(x) + Float.hashCode(y)) + Float.hashCode(z); }

    @Override public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
}
