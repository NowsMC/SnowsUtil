package space.nows.util.array;

import space.nows.util.check.Preconditions;

/** Whole-array arithmetic: sums and in-place prefix sums. Sums of {@code int[]} are exact (accumulated as {@code long}). */
public final class ArrayMath {
    private ArrayMath() {}

    public static long sum(int[] a) {
        long s = 0;
        for (int v : Preconditions.notNull(a, "a")) s += v;
        return s;
    }

    /** Sum with {@code long} wrap-around on overflow. */
    public static long sum(long[] a) {
        long s = 0;
        for (long v : Preconditions.notNull(a, "a")) s += v;
        return s;
    }

    /** Replaces each element with the sum of itself and all earlier elements (wraps on overflow); returns {@code a}. */
    public static int[] prefixSum(int[] a) {
        Preconditions.notNull(a, "a");
        for (int i = 1; i < a.length; i++) a[i] += a[i - 1];
        return a;
    }

    /** Replaces each element with the sum of itself and all earlier elements (wraps on overflow); returns {@code a}. */
    public static long[] prefixSum(long[] a) {
        Preconditions.notNull(a, "a");
        for (int i = 1; i < a.length; i++) a[i] += a[i - 1];
        return a;
    }
}
