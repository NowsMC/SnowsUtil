package space.nows.util.array;

import space.nows.util.check.Preconditions;

/** Copying whole arrays and ranges between arrays. */
public final class ArrayCopy {
    private ArrayCopy() {}

    public static byte[] copy(byte[] a) { return Preconditions.notNull(a, "a").clone(); }

    public static int[] copy(int[] a) { return Preconditions.notNull(a, "a").clone(); }

    public static long[] copy(long[] a) { return Preconditions.notNull(a, "a").clone(); }

    public static void copy(byte[] src, int srcPos, byte[] dst, int dstPos, int len) {
        Preconditions.notNull(src, "src");
        Preconditions.notNull(dst, "dst");
        Preconditions.range(srcPos, len, src.length);
        Preconditions.range(dstPos, len, dst.length);
        System.arraycopy(src, srcPos, dst, dstPos, len);
    }

    public static void copy(int[] src, int srcPos, int[] dst, int dstPos, int len) {
        Preconditions.notNull(src, "src");
        Preconditions.notNull(dst, "dst");
        Preconditions.range(srcPos, len, src.length);
        Preconditions.range(dstPos, len, dst.length);
        System.arraycopy(src, srcPos, dst, dstPos, len);
    }

    public static void copy(long[] src, int srcPos, long[] dst, int dstPos, int len) {
        Preconditions.notNull(src, "src");
        Preconditions.notNull(dst, "dst");
        Preconditions.range(srcPos, len, src.length);
        Preconditions.range(dstPos, len, dst.length);
        System.arraycopy(src, srcPos, dst, dstPos, len);
    }
}
