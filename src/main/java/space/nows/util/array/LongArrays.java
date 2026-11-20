package space.nows.util.array;

import java.util.Arrays;

/** Helpers for {@code long[]}: resize, swap, search. */
public final class LongArrays {
    private LongArrays() {}

    public static long[] resize(long[] a, int newLength) { return Arrays.copyOf(a, newLength); }

    public static void swap(long[] a, int i, int j) { long t = a[i]; a[i] = a[j]; a[j] = t; }

    public static int indexOf(long[] a, long value) {
        for (int i = 0; i < a.length; i++) if (a[i] == value) return i;
        return -1;
    }
}
