package space.nows.util.bits;

/** Finding set and clear bits in a {@code long[]} bit array. */
public final class BitSearch {
    private BitSearch() {}

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
        if (nbits < 0 || nbits > BitArrays.capacity(bits)) {
            throw new IllegalArgumentException("nbits " + nbits + " exceeds capacity " + BitArrays.capacity(bits));
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
        long cap = BitArrays.capacity(bits);
        if (from >= cap) from = (int) (cap - 1);
        int w = from >>> 6;
        long word = bits[w] & (-1L >>> -(from + 1));
        for (;;) {
            if (word != 0L) return (w << 6) + 63 - Long.numberOfLeadingZeros(word);
            if (w-- == 0) return -1;
            word = bits[w];
        }
    }
}
