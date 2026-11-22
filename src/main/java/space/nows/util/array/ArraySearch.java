package space.nows.util.array;

import space.nows.util.io.ByteAccess;

import java.util.Arrays;
import java.util.Objects;

/** Linear search for values in arrays, and sub-sequence search for {@code byte[]}. Generated from {@code ArraySearch.tpl}; edit the template, not this file. */
public final class ArraySearch {
    private ArraySearch() {}

    public static int indexOf(byte[] a, byte value) { return indexOf(a, value, 0); }

    public static int indexOf(int[] a, int value) { return indexOf(a, value, 0); }

    public static int indexOf(long[] a, long value) { return indexOf(a, value, 0); }

    /** First index {@code >= from} holding {@code value}, or -1. A negative {@code from} is treated as 0. */
    public static int indexOf(byte[] a, byte value, int from) {
        int n = a.length;
        int i = Math.max(from, 0);
        if (n - i >= 8) {
            long pattern = (value & 0xFFL) * 0x0101010101010101L;
            for (int limit = n - 8; i <= limit; i += 8) {
                long x = ByteAccess.readLongLE(a, i) ^ pattern;
                long hit = (x - 0x0101010101010101L) & ~x & 0x8080808080808080L;
                if (hit != 0L) return i + (Long.numberOfTrailingZeros(hit) >>> 3);
            }
        }
        for (; i < n; i++) if (a[i] == value) return i;
        return -1;
    }

    /** First index {@code >= from} holding {@code value}, or -1. A negative {@code from} is treated as 0. */
    public static int indexOf(int[] a, int value, int from) {
        for (int i = Math.max(from, 0); i < a.length; i++) if (a[i] == value) return i;
        return -1;
    }

    /** First index {@code >= from} holding {@code value}, or -1. A negative {@code from} is treated as 0. */
    public static int indexOf(long[] a, long value, int from) {
        for (int i = Math.max(from, 0); i < a.length; i++) if (a[i] == value) return i;
        return -1;
    }

    /** Uses {@code null}-safe {@code equals}. */
    public static <T> int indexOf(T[] a, Object value) {
        if (value == null) {
            for (int i = 0; i < a.length; i++) if (a[i] == null) return i;
        } else {
            for (int i = 0; i < a.length; i++) if (value.equals(a[i])) return i;
        }
        return -1;
    }

    public static int lastIndexOf(byte[] a, byte value) {
        for (int i = a.length - 1; i >= 0; i--) if (a[i] == value) return i;
        return -1;
    }

    public static int lastIndexOf(int[] a, int value) {
        for (int i = a.length - 1; i >= 0; i--) if (a[i] == value) return i;
        return -1;
    }

    public static int lastIndexOf(long[] a, long value) {
        for (int i = a.length - 1; i >= 0; i--) if (a[i] == value) return i;
        return -1;
    }

    public static <T> int lastIndexOf(T[] a, Object value) {
        for (int i = a.length - 1; i >= 0; i--) if (Objects.equals(value, a[i])) return i;
        return -1;
    }

    public static boolean contains(byte[] a, byte value) { return indexOf(a, value, 0) >= 0; }

    public static boolean contains(int[] a, int value) { return indexOf(a, value, 0) >= 0; }

    public static boolean contains(long[] a, long value) { return indexOf(a, value, 0) >= 0; }

    public static <T> boolean contains(T[] a, Object value) { return indexOf(a, value) >= 0; }

    public static int indexOf(byte[] data, byte[] pattern) { return indexOf(data, pattern, 0); }

    /** First index {@code >= from} where {@code pattern} occurs in {@code data}, or -1. An empty pattern matches at {@code from}. */
    public static int indexOf(byte[] data, byte[] pattern, int from) {
        int n = data.length, m = pattern.length;
        if (from < 0) from = 0;
        if (m == 0) return Math.min(from, n);
        byte first = pattern[0];
        int max = n - m;
        for (int i = indexOf(data, first, from); i >= 0 && i <= max; i = indexOf(data, first, i + 1)) {
            if (Arrays.equals(data, i + 1, i + m, pattern, 1, m)) return i;
        }
        return -1;
    }

    public static boolean contains(byte[] data, byte[] pattern) { return indexOf(data, pattern, 0) >= 0; }
}
