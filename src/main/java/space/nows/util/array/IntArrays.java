package space.nows.util.array;

import java.util.Arrays;

/** Helpers for {@code int[]}: resize, insert/remove, swap, search, reverse. */
public final class IntArrays {
    private IntArrays() {}

    public static final int[] EMPTY_INT = new int[0];

    public static int[] copy(int[] a) { return a.clone(); }

    public static void copy(int[] src, int srcPos, int[] dst, int dstPos, int len) {
        System.arraycopy(src, srcPos, dst, dstPos, len);
    }

    public static int[] resize(int[] a, int newLength) { return Arrays.copyOf(a, newLength); }

    public static int[] ensureCapacity(int[] a, int minCapacity) {
        if (a.length >= minCapacity) return a;
        long grown = Math.max((long) minCapacity, a.length + (a.length >> 1) + 1L);
        return Arrays.copyOf(a, (int) Math.min(grown, Integer.MAX_VALUE - 8));
    }

    public static int[] remove(int[] a, int index) {
        checkIndex(index, a.length);
        int[] out = new int[a.length - 1];
        System.arraycopy(a, 0, out, 0, index);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
        return out;
    }

    public static int swapRemove(int[] a, int index, int size) {
        checkIndex(index, size);
        a[index] = a[size - 1];
        return size - 1;
    }

    public static int[] insert(int[] a, int index, int value) {
        if (index < 0 || index > a.length) throw new IndexOutOfBoundsException("index " + index);
        int[] out = new int[a.length + 1];
        System.arraycopy(a, 0, out, 0, index);
        out[index] = value;
        System.arraycopy(a, index, out, index + 1, a.length - index);
        return out;
    }

    public static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    public static int indexOf(int[] a, int value) {
        for (int i = 0; i < a.length; i++) if (a[i] == value) return i;
        return -1;
    }

    public static boolean contains(int[] a, int value) { return indexOf(a, value) >= 0; }

    public static void reverse(int[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) { int t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    private static void checkIndex(int index, int length) {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException("index " + index + ", length " + length);
    }
}
