package space.nows.util.bits;

import space.nows.util.check.Preconditions;

import java.util.Arrays;

/**
 * Fixed-size bit arrays stored in {@code long[]} (bit {@code i} lives in word {@code i >>> 6}).
 * Every index is bounds-checked against the array, so a bad index can never wrap into another word.
 */
public final class BitArrays {
    private BitArrays() {}

    /** Allocates words for {@code nbits} bits. */
    public static long[] create(int nbits) {
        Preconditions.nonNegative(nbits, "nbits");
        return new long[(int) (((long) nbits + 63) >>> 6)];
    }

    /** Number of addressable bits ({@code bits.length * 64}). */
    public static long capacity(long[] bits) { return (long) bits.length << 6; }

    private static void checkBit(long[] bits, int i) {
        if (i < 0 || (i >>> 6) >= bits.length) {
            throw new IndexOutOfBoundsException("bit index " + i + " out of bounds for " + capacity(bits) + " bits");
        }
    }

    public static boolean get(long[] bits, int i) { checkBit(bits, i); return (bits[i >>> 6] & (1L << i)) != 0L; }

    public static void set(long[] bits, int i) { checkBit(bits, i); bits[i >>> 6] |= 1L << i; }

    public static void set(long[] bits, int i, boolean value) {
        if (value) set(bits, i); else clear(bits, i);
    }

    public static void clear(long[] bits, int i) { checkBit(bits, i); bits[i >>> 6] &= ~(1L << i); }

    public static void flip(long[] bits, int i) { checkBit(bits, i); bits[i >>> 6] ^= 1L << i; }

    public static void clearAll(long[] bits) { Arrays.fill(bits, 0L); }

    public static int count(long[] bits) {
        int c = 0;
        for (long w : bits) c += Long.bitCount(w);
        return c;
    }

    public static boolean isEmpty(long[] bits) {
        for (long w : bits) if (w != 0L) return false;
        return true;
    }
}
