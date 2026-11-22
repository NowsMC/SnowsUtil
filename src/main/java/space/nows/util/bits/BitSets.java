package space.nows.util.bits;

import space.nows.util.error.Checks;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Fixed-size bit sets stored in {@code long[]} (bit {@code i} lives in word {@code i >>> 6}).
 * Every index is bounds-checked against the array, so a bad index can never wrap into another word.
 */
public final class BitSets {
    private BitSets() {}

    /** Allocates words for {@code nbits} bits. */
    public static long[] create(int nbits) {
        Checks.nonNegative(nbits, "nbits");
        return new long[(int) (((long) nbits + 63) >>> 6)];
    }

    /** Number of addressable bits ({@code bits.length * 64}). */
    public static long capacity(long[] bits) { return (long) bits.length << 6; }

    private static void checkBit(long[] bits, int i) {
        if (i < 0 || (i >>> 6) >= bits.length) {
            throw new IndexOutOfBoundsException("bit index " + i + " out of bounds for " + capacity(bits) + " bits");
        }
    }

    private static void checkBitRange(long[] bits, int from, int to) {
        if (from < 0 || to < from || to > capacity(bits)) {
            throw new IndexOutOfBoundsException("bit range [" + from + ", " + to + ") out of bounds for " + capacity(bits) + " bits");
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

    /** Sets bits {@code [from, to)}. */
    public static void setRange(long[] bits, int from, int to) {
        checkBitRange(bits, from, to);
        if (from == to) return;
        int fw = from >>> 6, lw = (to - 1) >>> 6;
        long firstMask = -1L << from, lastMask = -1L >>> -to;
        if (fw == lw) {
            bits[fw] |= firstMask & lastMask;
        } else {
            bits[fw] |= firstMask;
            for (int w = fw + 1; w < lw; w++) bits[w] = -1L;
            bits[lw] |= lastMask;
        }
    }

    /** Clears bits {@code [from, to)}. */
    public static void clearRange(long[] bits, int from, int to) {
        checkBitRange(bits, from, to);
        if (from == to) return;
        int fw = from >>> 6, lw = (to - 1) >>> 6;
        long firstMask = -1L << from, lastMask = -1L >>> -to;
        if (fw == lw) {
            bits[fw] &= ~(firstMask & lastMask);
        } else {
            bits[fw] &= ~firstMask;
            for (int w = fw + 1; w < lw; w++) bits[w] = 0L;
            bits[lw] &= ~lastMask;
        }
    }

    public static int count(long[] bits) {
        int c = 0;
        for (long w : bits) c += Long.bitCount(w);
        return c;
    }

    public static boolean isEmpty(long[] bits) {
        for (long w : bits) if (w != 0L) return false;
        return true;
    }

    /** Index of the first set bit at or after {@code from}, or -1. */
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

    /** Index of the first clear bit in {@code [from, nbits)}, or -1. */
    public static int nextClearBit(long[] bits, int from, int nbits) {
        if (from < 0) throw new IndexOutOfBoundsException("from < 0");
        if (nbits < 0 || nbits > capacity(bits)) {
            throw new IllegalArgumentException("nbits " + nbits + " exceeds capacity " + capacity(bits));
        }
        if (from >= nbits) return -1;
        int w = from >>> 6;
        long word = ~bits[w] & (-1L << from);
        for (;;) {
            if (word != 0L) {
                int r = (w << 6) + Long.numberOfTrailingZeros(word);
                return r < nbits ? r : -1;
            }
            if (++w == bits.length) return -1;
            word = ~bits[w];
        }
    }

    /** Index of the last set bit at or before {@code from}, or -1. {@code from == -1} is allowed and returns -1. */
    public static int previousSetBit(long[] bits, int from) {
        if (from < 0) {
            if (from == -1) return -1;
            throw new IndexOutOfBoundsException("from < -1: " + from);
        }
        if (bits.length == 0) return -1;
        long cap = capacity(bits);
        if (from >= cap) from = (int) (cap - 1);
        int w = from >>> 6;
        long word = bits[w] & (-1L >>> -(from + 1));
        for (;;) {
            if (word != 0L) return (w << 6) + 63 - Long.numberOfLeadingZeros(word);
            if (w-- == 0) return -1;
            word = bits[w];
        }
    }

    private static void sameSize(long[] a, long[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("bit sets must have the same length: " + a.length + " vs " + b.length);
        }
    }

    /** {@code dst |= src}. */
    public static void or(long[] dst, long[] src) { sameSize(dst, src); for (int i = 0; i < dst.length; i++) dst[i] |= src[i]; }

    /** {@code dst &= src}. */
    public static void and(long[] dst, long[] src) { sameSize(dst, src); for (int i = 0; i < dst.length; i++) dst[i] &= src[i]; }

    /** {@code dst &= ~src}. */
    public static void andNot(long[] dst, long[] src) { sameSize(dst, src); for (int i = 0; i < dst.length; i++) dst[i] &= ~src[i]; }

    /** {@code dst ^= src}. */
    public static void xor(long[] dst, long[] src) { sameSize(dst, src); for (int i = 0; i < dst.length; i++) dst[i] ^= src[i]; }

    public static boolean intersects(long[] a, long[] b) {
        sameSize(a, b);
        for (int i = 0; i < a.length; i++) if ((a[i] & b[i]) != 0L) return true;
        return false;
    }

    /** Copies into a JDK {@link BitSet}. */
    public static BitSet toBitSet(long[] bits) { return BitSet.valueOf(bits); }

    /** Copies a JDK {@link BitSet} into a new array sized for {@code nbits} bits. */
    public static long[] fromBitSet(BitSet source, int nbits) {
        Checks.notNull(source, "source");
        if (source.length() > nbits) {
            throw new IllegalArgumentException("bit set has bits beyond nbits=" + nbits + " (length " + source.length() + ")");
        }
        long[] out = create(nbits);
        long[] words = source.toLongArray();
        System.arraycopy(words, 0, out, 0, words.length);
        return out;
    }
}
