package space.nows.util.array;

public final class BitUtils {
    private BitUtils() {}

    public static long[] create(int nbits) { return new long[(nbits + 63) >>> 6]; }

    public static boolean get(long[] bits, int i) { return (bits[i >>> 6] & (1L << i)) != 0L; }

    public static void set(long[] bits, int i) { bits[i >>> 6] |= 1L << i; }

    public static void set(long[] bits, int i, boolean value) {
        if (value) set(bits, i); else clear(bits, i);
    }

    public static void clear(long[] bits, int i) { bits[i >>> 6] &= ~(1L << i); }

    public static void flip(long[] bits, int i) { bits[i >>> 6] ^= 1L << i; }

    public static void clearAll(long[] bits) { java.util.Arrays.fill(bits, 0L); }

    public static int count(long[] bits) {
        int c = 0;
        for (long w : bits) c += Long.bitCount(w);
        return c;
    }

    public static int nextSetBit(long[] bits, int from) {
        if (from < 0) throw new IndexOutOfBoundsException("from < 0");
        int w = from >>> 6;
        if (w >= bits.length) return -1;
        long word = bits[w] & (-1L << from);
        for (;;) {
            if (word != 0L) return (w << 6) + Long.numberOfTrailingZeros(word);
            if (++w == bits.length) return -1;
            word = bits[w];
        }
    }

    public static int nextClearBit(long[] bits, int from, int nbits) {
        for (int i = from; i < nbits; i++) if (!get(bits, i)) return i;
        return -1;
    }

    public static boolean isSet(int value, int bit) { return (value & (1 << bit)) != 0; }

    public static int setBit(int value, int bit) { return value | (1 << bit); }

    public static int clearBit(int value, int bit) { return value & ~(1 << bit); }

    public static int toggleBit(int value, int bit) { return value ^ (1 << bit); }

    public static int count(int value) { return Integer.bitCount(value); }

    public static int lowestSetBit(int value) { return value & -value; }
}
