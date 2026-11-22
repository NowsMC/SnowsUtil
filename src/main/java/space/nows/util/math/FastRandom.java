package space.nows.util.math;

import space.nows.util.check.Preconditions;

import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;

/**
 * Small, fast, seedable xorshift128+ generator. Implements the JDK's {@link RandomGenerator}, so it also provides
 * {@code ints()}, {@code doubles()}, bounded {@code nextLong}, etc. Not thread-safe and not cryptographically secure.
 */
public final class FastRandom implements RandomGenerator {
    private static final AtomicLong SEED_UNIQUIFIER = new AtomicLong(8682522807148012L);

    private long s0, s1;

    public FastRandom() { this(SEED_UNIQUIFIER.updateAndGet(x -> x * 1181783497276652981L) ^ System.nanoTime()); }

    public FastRandom(long seed) { setSeed(seed); }

    private FastRandom(long s0, long s1) { this.s0 = s0; this.s1 = s1; }

    /** Independent generator that continues with the same sequence. */
    public FastRandom copy() { return new FastRandom(s0, s1); }

    public void setSeed(long seed) {
        long z = seed;
        s0 = splitMix(z += 0x9E3779B97F4A7C15L);
        s1 = splitMix(z + 0x9E3779B97F4A7C15L);
        if (s0 == 0 && s1 == 0) s1 = 1;
    }

    private static long splitMix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    @Override public long nextLong() {
        long x = s0, y = s1;
        s0 = y;
        x ^= x << 23;
        s1 = x ^ y ^ (x >>> 17) ^ (y >>> 26);
        return s1 + y;
    }

    @Override public int nextInt() { return (int) (nextLong() >>> 32); }

    @Override public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive: " + bound);
        return (int) (((nextLong() >>> 32) * bound) >>> 32);
    }

    /** Uniform in {@code [min, max)}; any range up to the full {@code int} span is supported (no overflow). */
    @Override public int nextInt(int min, int max) {
        if (min >= max) throw new IllegalArgumentException("min >= max: " + min + " >= " + max);
        long range = (long) max - min;
        if (range <= Integer.MAX_VALUE) return min + nextInt((int) range);
        int r;
        do { r = nextInt(); } while (r < min || r >= max);
        return r;
    }

    @Override public boolean nextBoolean() { return nextLong() < 0; }

    @Override public float nextFloat() { return (nextLong() >>> 40) * 0x1.0p-24f; }

    @Override public double nextDouble() { return (nextLong() >>> 11) * 0x1.0p-53; }

    /** Uniform in {@code [min, max)}. */
    @Override public float nextFloat(float min, float max) {
        float range = max - min;
        if (!(range > 0f) || Float.isInfinite(range)) {
            throw new IllegalArgumentException("need finite min < max with a finite range: " + min + ", " + max);
        }
        float r = min + nextFloat() * range;
        return r < max ? r : Math.nextDown(max);
    }

    @Override public double nextGaussian() {
        double u1 = 1.0 - nextDouble(), u2 = nextDouble();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    @Override public void nextBytes(byte[] out) {
        Preconditions.notNull(out, "out");
        int i = 0;
        while (i + 8 <= out.length) {
            long v = nextLong();
            for (int k = 0; k < 8; k++) { out[i++] = (byte) v; v >>>= 8; }
        }
        if (i < out.length) {
            long v = nextLong();
            while (i < out.length) { out[i++] = (byte) v; v >>>= 8; }
        }
    }

    /** Fisher-Yates shuffle. */
    public void shuffle(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }

    public void shuffle(long[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            long t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }

    public <T> void shuffle(T[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }
}
