package space.nows.util.array;

import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.Checks;

import java.util.Arrays;
import java.util.NoSuchElementException;

/** Helpers for {@code long[]}: copy, resize, insert/remove, swap, search, reverse, min/max, concat. */
public final class LongArrays {
    private LongArrays() {}

    private static final int MAX_LENGTH = Integer.MAX_VALUE - 8;

    public static final long[] EMPTY_LONG = new long[0];

    public static long[] copy(long[] a) { return Checks.notNull(a, "a").clone(); }

    public static void copy(long[] src, int srcPos, long[] dst, int dstPos, int len) {
        Checks.notNull(src, "src");
        Checks.notNull(dst, "dst");
        Checks.range(srcPos, len, src.length);
        Checks.range(dstPos, len, dst.length);
        System.arraycopy(src, srcPos, dst, dstPos, len);
    }

    public static long[] resize(long[] a, int newLength) {
        return Arrays.copyOf(Checks.notNull(a, "a"), Checks.nonNegative(newLength, "newLength"));
    }

    /** Returns {@code a} if it is already large enough, otherwise a copy grown by ~1.5x (at least {@code minCapacity}). */
    public static long[] ensureCapacity(long[] a, int minCapacity) {
        Checks.notNull(a, "a");
        Checks.nonNegative(minCapacity, "minCapacity");
        if (a.length >= minCapacity) return a;
        if (minCapacity > MAX_LENGTH) throw new CapacityExceededException("array", minCapacity, MAX_LENGTH);
        long grown = Math.max((long) minCapacity, a.length + (a.length >> 1) + 1L);
        return Arrays.copyOf(a, (int) Math.min(grown, MAX_LENGTH));
    }

    public static long[] remove(long[] a, int index) {
        Checks.index(index, Checks.notNull(a, "a").length);
        long[] out = new long[a.length - 1];
        System.arraycopy(a, 0, out, 0, index);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
        return out;
    }

    /** Overwrites {@code index} with the last live element and returns the new logical size. Order is not kept. */
    public static int swapRemove(long[] a, int index, int size) {
        Checks.position(size, Checks.notNull(a, "a").length);
        Checks.index(index, size);
        a[index] = a[size - 1];
        return size - 1;
    }

    public static long[] insert(long[] a, int index, long value) {
        Checks.position(index, Checks.notNull(a, "a").length);
        long[] out = new long[checkedLength(a.length + 1L)];
        System.arraycopy(a, 0, out, 0, index);
        out[index] = value;
        System.arraycopy(a, index, out, index + 1, a.length - index);
        return out;
    }

    public static long[] append(long[] a, long value) {
        long[] out = Arrays.copyOf(Checks.notNull(a, "a"), checkedLength(a.length + 1L));
        out[a.length] = value;
        return out;
    }

    public static long[] concat(long[] a, long[] b) {
        Checks.notNull(a, "a");
        Checks.notNull(b, "b");
        long[] out = Arrays.copyOf(a, checkedLength((long) a.length + b.length));
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static void swap(long[] a, int i, int j) { long t = a[i]; a[i] = a[j]; a[j] = t; }

    public static int indexOf(long[] a, long value) { return indexOf(a, value, 0); }

    /** First index {@code >= from} holding {@code value}, or -1. A negative {@code from} is treated as 0. */
    public static int indexOf(long[] a, long value, int from) {
        for (int i = Math.max(from, 0); i < a.length; i++) if (a[i] == value) return i;
        return -1;
    }

    public static int lastIndexOf(long[] a, long value) {
        for (int i = a.length - 1; i >= 0; i--) if (a[i] == value) return i;
        return -1;
    }

    public static boolean contains(long[] a, long value) { return indexOf(a, value, 0) >= 0; }

    public static void reverse(long[] a) { reverse(a, 0, a.length); }

    /** Reverses {@code [from, to)}. */
    public static void reverse(long[] a, int from, int to) {
        java.util.Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { long t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    public static boolean isSorted(long[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static long min(long[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        long m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] < m) m = a[i];
        return m;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static long max(long[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        long m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] > m) m = a[i];
        return m;
    }

    private static int checkedLength(long n) {
        if (n > MAX_LENGTH) throw new CapacityExceededException("array", n, MAX_LENGTH);
        return (int) n;
    }
}
