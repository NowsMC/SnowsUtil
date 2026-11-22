package space.nows.util.array;

import java.util.Objects;

/** In-place reordering: swapping two elements and reversing a range. */
public final class ArrayReorder {
    private ArrayReorder() {}

    public static void swap(byte[] a, int i, int j) { byte t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void swap(long[] a, int i, int j) { long t = a[i]; a[i] = a[j]; a[j] = t; }

    public static <T> void swap(T[] a, int i, int j) { T t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void reverse(byte[] a) { reverse(a, 0, a.length); }

    /** Reverses {@code [from, to)}. */
    public static void reverse(byte[] a, int from, int to) {
        Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { byte t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    public static void reverse(int[] a) { reverse(a, 0, a.length); }

    /** Reverses {@code [from, to)}. */
    public static void reverse(int[] a, int from, int to) {
        Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { int t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    public static void reverse(long[] a) { reverse(a, 0, a.length); }

    /** Reverses {@code [from, to)}. */
    public static void reverse(long[] a, int from, int to) {
        Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { long t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    public static <T> void reverse(T[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) { T t = a[i]; a[i] = a[j]; a[j] = t; }
    }
}
