package space.nows.util.array;

import java.util.Objects;

/** Linear search for values in arrays, and sub-sequence search for {@code byte[]}. */
public final class ArraySearch {
    private ArraySearch() {}

    public static int indexOf(byte[] a, byte value) { return indexOf(a, value, 0); }

    /** First index {@code >= from} holding {@code value}, or -1. A negative {@code from} is treated as 0. */
    public static int indexOf(byte[] a, byte value, int from) {
        for (int i = Math.max(from, 0); i < a.length; i++) if (a[i] == value) return i;
        return -1;
    }

    public static int lastIndexOf(byte[] a, byte value) {
        for (int i = a.length - 1; i >= 0; i--) if (a[i] == value) return i;
        return -1;
    }

    public static boolean contains(byte[] a, byte value) { return indexOf(a, value, 0) >= 0; }

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

    /** Uses {@code null}-safe {@code equals}. */
    public static <T> int indexOf(T[] a, Object value) {
        if (value == null) {
            for (int i = 0; i < a.length; i++) if (a[i] == null) return i;
        } else {
            for (int i = 0; i < a.length; i++) if (value.equals(a[i])) return i;
        }
        return -1;
    }

    public static <T> int lastIndexOf(T[] a, Object value) {
        for (int i = a.length - 1; i >= 0; i--) if (Objects.equals(value, a[i])) return i;
        return -1;
    }

    public static <T> boolean contains(T[] a, Object value) { return indexOf(a, value) >= 0; }

    public static int indexOf(byte[] data, byte[] pattern) { return indexOf(data, pattern, 0); }

    /** First index {@code >= from} where {@code pattern} occurs in {@code data}, or -1. An empty pattern matches at {@code from}. */
    public static int indexOf(byte[] data, byte[] pattern, int from) {
        int n = data.length, m = pattern.length;
        if (from < 0) from = 0;
        if (m == 0) return Math.min(from, n);
        byte first = pattern[0];
        for (int i = from, max = n - m; i <= max; i++) {
            if (data[i] != first) continue;
            int j = 1;
            while (j < m && data[i + j] == pattern[j]) j++;
            if (j == m) return i;
        }
        return -1;
    }

    public static boolean contains(byte[] data, byte[] pattern) { return indexOf(data, pattern, 0) >= 0; }
}
