package space.nows.util.array;

import space.nows.util.check.Preconditions;

import java.util.Arrays;

/** Changing array length: exact resize and amortized capacity growth. */
public final class ArrayResize {
    private ArrayResize() {}

    public static byte[] resize(byte[] a, int newLength) {
        return Arrays.copyOf(Preconditions.notNull(a, "a"), Preconditions.nonNegative(newLength, "newLength"));
    }

    public static int[] resize(int[] a, int newLength) {
        return Arrays.copyOf(Preconditions.notNull(a, "a"), Preconditions.nonNegative(newLength, "newLength"));
    }

    public static long[] resize(long[] a, int newLength) {
        return Arrays.copyOf(Preconditions.notNull(a, "a"), Preconditions.nonNegative(newLength, "newLength"));
    }

    public static <T> T[] resize(T[] a, int newLength) {
        return Arrays.copyOf(Preconditions.notNull(a, "a"), Preconditions.nonNegative(newLength, "newLength"));
    }

    /** Returns {@code a} if it is already large enough, otherwise a copy grown by ~1.5x (at least {@code minCapacity}). */
    public static byte[] ensureCapacity(byte[] a, int minCapacity) {
        Preconditions.notNull(a, "a");
        Preconditions.nonNegative(minCapacity, "minCapacity");
        if (a.length >= minCapacity) return a;
        return Arrays.copyOf(a, ArrayLimits.grownLength(a.length, minCapacity));
    }

    public static int[] ensureCapacity(int[] a, int minCapacity) {
        Preconditions.notNull(a, "a");
        Preconditions.nonNegative(minCapacity, "minCapacity");
        if (a.length >= minCapacity) return a;
        return Arrays.copyOf(a, ArrayLimits.grownLength(a.length, minCapacity));
    }

    public static long[] ensureCapacity(long[] a, int minCapacity) {
        Preconditions.notNull(a, "a");
        Preconditions.nonNegative(minCapacity, "minCapacity");
        if (a.length >= minCapacity) return a;
        return Arrays.copyOf(a, ArrayLimits.grownLength(a.length, minCapacity));
    }

    public static <T> T[] ensureCapacity(T[] a, int minCapacity) {
        Preconditions.notNull(a, "a");
        Preconditions.nonNegative(minCapacity, "minCapacity");
        if (a.length >= minCapacity) return a;
        return Arrays.copyOf(a, ArrayLimits.grownLength(a.length, minCapacity));
    }
}
