package space.nows.util.random;

import java.util.random.RandomGenerator;

/**
 * Fast, seedable xoshiro256** generator with 256 bits of state. Every {@link RandomGenerator} method is
 * inherited, so bounded values, ranges, floating point values, Gaussians and streams are uniform and unbiased by
 * the JDK's own definitions. Not thread-safe and not cryptographically secure; use {@link TrueRandom} for that.
 */
public final class FastRandom implements RandomGenerator {
    private long s0, s1, s2, s3;

    /** Seeds the full state from the operating system's entropy source. */
    public FastRandom() {
        TrueRandom source = TrueRandom.shared();
        s0 = source.nextLong();
        s1 = source.nextLong();
        s2 = source.nextLong();
        s3 = source.nextLong();
        if ((s0 | s1 | s2 | s3) == 0L) s0 = 1L;
    }

    /** Deterministic: the same seed always gives the same sequence. */
    public FastRandom(long seed) { setSeed(seed); }

    private FastRandom(FastRandom o) {
        s0 = o.s0;
        s1 = o.s1;
        s2 = o.s2;
        s3 = o.s3;
    }

    /** Independent generator that continues with the same sequence. */
    public FastRandom copy() { return new FastRandom(this); }

    public void setSeed(long seed) {
        long z = seed;
        s0 = splitMix(z += 0x9E3779B97F4A7C15L);
        s1 = splitMix(z += 0x9E3779B97F4A7C15L);
        s2 = splitMix(z += 0x9E3779B97F4A7C15L);
        s3 = splitMix(z + 0x9E3779B97F4A7C15L);
    }

    private static long splitMix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    @Override public long nextLong() {
        long result = Long.rotateLeft(s1 * 5, 7) * 9;
        long t = s1 << 17;
        s2 ^= s0;
        s3 ^= s1;
        s1 ^= s2;
        s0 ^= s3;
        s2 ^= t;
        s3 = Long.rotateLeft(s3, 45);
        return result;
    }
}
