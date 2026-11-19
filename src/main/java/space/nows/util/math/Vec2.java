package space.nows.util.math;

public final class Vec2 {
    public float x, y;

    public Vec2() {}

    public Vec2(float x, float y) { this.x = x; this.y = y; }

    public Vec2(Vec2 o) { this.x = o.x; this.y = o.y; }

    public Vec2 set(float x, float y) { this.x = x; this.y = y; return this; }

    public Vec2 set(Vec2 o) { this.x = o.x; this.y = o.y; return this; }

    public Vec2 add(float ox, float oy) { x += ox; y += oy; return this; }

    public Vec2 add(Vec2 o) { x += o.x; y += o.y; return this; }

    public Vec2 sub(float ox, float oy) { x -= ox; y -= oy; return this; }

    public Vec2 sub(Vec2 o) { x -= o.x; y -= o.y; return this; }

    public Vec2 scale(float s) { x *= s; y *= s; return this; }

    public Vec2 negate() { x = -x; y = -y; return this; }

    public float dot(Vec2 o) { return x * o.x + y * o.y; }

    public float cross(Vec2 o) { return x * o.y - y * o.x; }

    public float lengthSq() { return x * x + y * y; }

    public float length() { return (float) Math.sqrt(x * x + y * y); }

    public float distanceSq(Vec2 o) { float dx = x - o.x, dy = y - o.y; return dx * dx + dy * dy; }

    public float distance(Vec2 o) { return (float) Math.sqrt(distanceSq(o)); }

    public Vec2 normalize() {
        float len = length();
        if (len != 0f) { float inv = 1f / len; x *= inv; y *= inv; }
        return this;
    }

    public Vec2 lerp(Vec2 target, float t) { x += (target.x - x) * t; y += (target.y - y) * t; return this; }

    public Vec2 rotate(float radians) {
        float c = (float) Math.cos(radians), s = (float) Math.sin(radians);
        float nx = x * c - y * s, ny = x * s + y * c;
        x = nx; y = ny;
        return this;
    }

    public Vec2 copy() { return new Vec2(x, y); }

    @Override public boolean equals(Object o) {
        return o instanceof Vec2 v && Float.compare(x, v.x) == 0 && Float.compare(y, v.y) == 0;
    }

    @Override public int hashCode() { return 31 * Float.hashCode(x) + Float.hashCode(y); }

    @Override public String toString() { return "(" + x + ", " + y + ")"; }
}
