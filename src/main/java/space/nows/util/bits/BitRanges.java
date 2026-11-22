package space.nows.util.bits;

/** Setting and clearing contiguous runs of bits in a {@code long[]} bit array, touching whole words where possible. */
public final class BitRanges {
    private BitRanges() {}

    /** Sets bits {@code [from, to)}. */
    public static void setRange(long[] bits, int from, int to) {
        checkRange(bits, from, to);
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
        checkRange(bits, from, to);
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

    private static void checkRange(long[] bits, int from, int to) {
        if (from < 0 || to < from || to > BitArrays.capacity(bits)) {
            throw new IndexOutOfBoundsException("bit range [" + from + ", " + to + ") out of bounds for "
                    + BitArrays.capacity(bits) + " bits");
        }
    }
}
