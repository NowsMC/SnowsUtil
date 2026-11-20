package space.nows.util.math.linear;

public final class Vec4 {
    public float x, y, z, w;

    public Vec4() {}

    public Vec4(float x, float y, float z, float w) { this.x = x; this.y = y; this.z = z; this.w = w; }

    public Vec4 set(float x, float y, float z, float w) { this.x = x; this.y = y; this.z = z; this.w = w; return this; }

    public Vec4 set(Vec4 o) { return set(o.x, o.y, o.z, o.w); }

    public Vec4 add(Vec4 o) { x += o.x; y += o.y; z += o.z; w += o.w; return this; }

    public Vec4 sub(Vec4 o) { x -= o.x; y -= o.y; z -= o.z; w -= o.w; return this; }

    public Vec4 scale(float s) { x *= s; y *= s; z *= s; w *= s; return this; }

    public float dot(Vec4 o) { return x * o.x + y * o.y + z * o.z + w * o.w; }

    public float lengthSq() { return x * x + y * y + z * z + w * w; }

    public float length() { return (float) Math.sqrt(lengthSq()); }

    public Vec4 normalize() {
        float len = length();
        if (len != 0f) { float inv = 1f / len; x *= inv; y *= inv; z *= inv; w *= inv; }
        return this;
    }

    public Vec4 copy() { return new Vec4(x, y, z, w); }

    @Override public boolean equals(Object o) {
        return o instanceof Vec4 v && Float.compare(x, v.x) == 0 && Float.compare(y, v.y) == 0
                && Float.compare(z, v.z) == 0 && Float.compare(w, v.w) == 0;
    }

    @Override public int hashCode() {
        int h = Float.hashCode(x);
        h = 31 * h + Float.hashCode(y);
        h = 31 * h + Float.hashCode(z);
        return 31 * h + Float.hashCode(w);
    }

    @Override public String toString() { return "(" + x + ", " + y + ", " + z + ", " + w + ")"; }
}
