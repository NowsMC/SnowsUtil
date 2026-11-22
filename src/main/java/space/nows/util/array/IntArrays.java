package space.nows.util.array;

import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.Checks;

import java.util.Arrays;
import java.util.NoSuchElementException;

/** Helpers for {@code int[]}: copy, resize, insert/remove, swap, search, reverse, min/max, concat. */
public final class IntArrays {
    private IntArrays() {}

    private static final int MAX_LENGTH = Integer.MAX_VALUE - 8;

    public static final int[] EMPTY_INT = new int[0];

    public static int[] copy(int[] a) { return Checks.notNull(a, "a").clone(); }

    public static void copy(int[] src, int srcPos, int[] dst, int dstPos, int len) {
        Checks.notNull(src, "src");
        Checks.notNull(dst, "dst");
        Checks.range(srcPos, len, src.length);
        Checks.range(dstPos, len, dst.length);
        System.arraycopy(src, srcPos, dst, dstPos, len);
    }

    public static int[] resize(int[] a, int newLength) {
        return Arrays.copyOf(Checks.notNull(a, "a"), Checks.nonNegative(newLength, "newLength"));
    }

    /** Returns {@code a} if it is already large enough, otherwise a copy grown by ~1.5x (at least {@code minCapacity}). */
    public static int[] ensureCapacity(int[] a, int minCapacity) {
        Checks.notNull(a, "a");
        Checks.nonNegative(minCapacity, "minCapacity");
        if (a.length >= minCapacity) return a;
        if (minCapacity > MAX_LENGTH) throw new CapacityExceededException("array", minCapacity, MAX_LENGTH);
        long grown = Math.max((long) minCapacity, a.length + (a.length >> 1) + 1L);
        return Arrays.copyOf(a, (int) Math.min(grown, MAX_LENGTH));
    }

    public static int[] remove(int[] a, int index) {
        Checks.index(index, Checks.notNull(a, "a").length);
        int[] out = new int[a.length - 1];
        System.arraycopy(a, 0, out, 0, index);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
        return out;
    }

    /** Overwrites {@code index} with the last live element and returns the new logical size. Order is not kept. */
    public static int swapRemove(int[] a, int index, int size) {
        Checks.position(size, Checks.notNull(a, "a").length);
        Checks.index(index, size);
        a[index] = a[size - 1];
        return size - 1;
    }

    public static int[] insert(int[] a, int index, int value) {
        Checks.position(index, Checks.notNull(a, "a").length);
        int[] out = new int[checkedLength(a.length + 1L)];
        System.arraycopy(a, 0, out, 0, index);
        out[index] = value;
        System.arraycopy(a, index, out, index + 1, a.length - index);
        return out;
    }

    public static int[] append(int[] a, int value) {
        int[] out = Arrays.copyOf(Checks.notNull(a, "a"), checkedLength(a.length + 1L));
        out[a.length] = value;
        return out;
    }

    public static int[] concat(int[] a, int[] b) {
        Checks.notNull(a, "a");
        Checks.notNull(b, "b");
        int[] out = Arrays.copyOf(a, checkedLength((long) a.length + b.length));
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    public static int indexOf(int[] a, int value) { return indexOf(a, value, 0); }

    /** First index {@code >= from} holding {@code value}, or -1. A negative {@code from} is treated as 0. */
    public static int indexOf(int[] a, int value, int from) {
        for (int i = Math.max(from, 0); i < a.length; i++) if (a[i] == value) return i;
        return -1;
    }

    public static int lastIndexOf(int[] a, int value) {
        for (int i = a.length - 1; i >= 0; i--) if (a[i] == value) return i;
        return -1;
    }

    public static boolean contains(int[] a, int value) { return indexOf(a, value, 0) >= 0; }

    public static void reverse(int[] a) { reverse(a, 0, a.length); }

    /** Reverses {@code [from, to)}. */
    public static void reverse(int[] a, int from, int to) {
        java.util.Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { int t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    public static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static int min(int[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        int m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] < m) m = a[i];
        return m;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static int max(int[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        int m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] > m) m = a[i];
        return m;
    }

    private static int checkedLength(long n) {
        if (n > MAX_LENGTH) throw new CapacityExceededException("array", n, MAX_LENGTH);
        return (int) n;
    }
}
