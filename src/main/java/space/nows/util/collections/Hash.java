package space.nows.util.collections;

final class Hash {
    private Hash() {}

    static int mix(int x) {
        int h = x * 0x9E3779B9;
        return h ^ (h >>> 16);
    }

    static int mix(long x) {
        long h = x * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32));
    }

    static void checkLoadFactor(float lf) {
        if (!(lf > 0f && lf < 1f)) throw new IllegalArgumentException("loadFactor must be in (0,1): " + lf);
    }

    static int capacityFor(int expected, float lf) {
        if (expected < 0) throw new IllegalArgumentException("expected < 0");
        long need = (long) Math.ceil(expected / (double) lf) + 1;
        int cap = 4;
        while (cap < need) {
            if (cap >= (1 << 30)) throw new IllegalStateException("capacity too large");
            cap <<= 1;
        }
        return cap;
    }

    static int threshold(int cap, float lf) {
        return Math.min(cap - 1, (int) (cap * lf));
    }
}
