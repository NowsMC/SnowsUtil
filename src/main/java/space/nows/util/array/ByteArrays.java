package space.nows.util.array;

import java.util.Arrays;

/** Helpers for {@code byte[]}: resize, swap, unsigned compare, equality and byte/pattern search. */
public final class ByteArrays {
    private ByteArrays() {}

    public static byte[] resize(byte[] a, int newLength) { return Arrays.copyOf(a, newLength); }

    public static void swap(byte[] a, int i, int j) { byte t = a[i]; a[i] = a[j]; a[j] = t; }

    public static int compare(byte[] a, byte[] b) { return Arrays.compareUnsigned(a, b); }

    public static boolean equals(byte[] a, byte[] b) { return Arrays.equals(a, b); }

    public static int indexOf(byte[] data, byte value) {
        for (int i = 0; i < data.length; i++) if (data[i] == value) return i;
        return -1;
    }

    public static int indexOf(byte[] data, byte[] pattern) { return indexOf(data, pattern, 0); }

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
}
