package space.nows.util.array;

import java.util.NoSuchElementException;

/** Ordering queries: sortedness, minimum, maximum and unsigned comparison of {@code byte[]}. */
public final class ArrayOrder {
    private ArrayOrder() {}

    public static boolean isSorted(byte[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    public static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    public static boolean isSorted(long[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static byte min(byte[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        byte m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] < m) m = a[i];
        return m;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static int min(int[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        int m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] < m) m = a[i];
        return m;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static long min(long[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        long m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] < m) m = a[i];
        return m;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static byte max(byte[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        byte m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] > m) m = a[i];
        return m;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static int max(int[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        int m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] > m) m = a[i];
        return m;
    }

    /** @throws NoSuchElementException if the array is empty */
    public static long max(long[] a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        long m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] > m) m = a[i];
        return m;
    }

    /** Lexicographic comparison treating each byte as unsigned. */
    public static int compareUnsigned(byte[] a, byte[] b) { return java.util.Arrays.compareUnsigned(a, b); }
}
