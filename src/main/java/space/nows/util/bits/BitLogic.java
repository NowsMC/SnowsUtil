package space.nows.util.bits;

/** Word-wise combination of two {@code long[]} bit arrays of the same length. The first argument is modified in place. */
public final class BitLogic {
    private BitLogic() {}

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
}
