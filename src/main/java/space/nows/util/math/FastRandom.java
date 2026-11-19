package space.nows.util.math;

public final class FastRandom {
    private long s0, s1;

    public FastRandom() { this(System.nanoTime() ^ 0x5DEECE66DL); }

    public FastRandom(long seed) { setSeed(seed); }

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

    public long nextLong() {
        long x = s0, y = s1;
        s0 = y;
        x ^= x << 23;
        s1 = x ^ y ^ (x >>> 17) ^ (y >>> 26);
        return s1 + y;
    }

    public int nextInt() { return (int) (nextLong() >>> 32); }

    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        return (int) (((nextLong() >>> 32) * bound) >>> 32);
    }

    public int nextInt(int min, int max) {
        if (min >= max) throw new IllegalArgumentException("min >= max");
        return min + nextInt(max - min);
    }

    public boolean nextBoolean() { return nextLong() < 0; }

    public float nextFloat() { return (nextLong() >>> 40) * 0x1.0p-24f; }

    public double nextDouble() { return (nextLong() >>> 11) * 0x1.0p-53; }

    public float nextFloat(float min, float max) { return min + nextFloat() * (max - min); }

    public double nextGaussian() {
        double u1 = 1.0 - nextDouble(), u2 = nextDouble();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    public void nextBytes(byte[] out) {
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

    public void shuffle(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }
}
