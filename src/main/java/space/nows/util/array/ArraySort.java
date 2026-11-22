package space.nows.util.array;

import space.nows.util.check.Preconditions;

import java.util.Arrays;
import java.util.Objects;

/**
 * LSD radix sorts for {@code int[]} and {@code long[]}. They allocate two work arrays the size of the range and
 * beat {@link Arrays#sort} on large inputs; small ranges fall back to the JDK sort. Results are in ascending
 * numeric order, exactly like {@link Arrays#sort}.
 */
public final class ArraySort {
    private ArraySort() {}

    private static final int RADIX_THRESHOLD = 1024;

    public static void radixSort(int[] a) { radixSort(a, 0, Preconditions.notNull(a, "a").length); }

    /** Sorts {@code [from, to)}. */
    public static void radixSort(int[] a, int from, int to) {
        Preconditions.notNull(a, "a");
        Objects.checkFromToIndex(from, to, a.length);
        int n = to - from;
        if (n < RADIX_THRESHOLD) {
            Arrays.sort(a, from, to);
            return;
        }
        int[] src = Arrays.copyOfRange(a, from, to);
        int[] dst = new int[n];
        int[][] counts = new int[4][256];
        for (int v : src) {
            counts[0][v & 0xFF]++;
            counts[1][(v >>> 8) & 0xFF]++;
            counts[2][(v >>> 16) & 0xFF]++;
            counts[3][((v >>> 24) & 0xFF) ^ 0x80]++;
        }
        for (int pass = 0; pass < 4; pass++) {
            int[] c = counts[pass];
            int shift = pass << 3;
            int flip = pass == 3 ? 0x80 : 0;
            if (c[((src[0] >>> shift) & 0xFF) ^ flip] == n) continue;
            int sum = 0;
            for (int i = 0; i < 256; i++) { int t = c[i]; c[i] = sum; sum += t; }
            for (int v : src) dst[c[((v >>> shift) & 0xFF) ^ flip]++] = v;
            int[] t = src; src = dst; dst = t;
        }
        System.arraycopy(src, 0, a, from, n);
    }

    public static void radixSort(long[] a) { radixSort(a, 0, Preconditions.notNull(a, "a").length); }

    /** Sorts {@code [from, to)}. */
    public static void radixSort(long[] a, int from, int to) {
        Preconditions.notNull(a, "a");
        Objects.checkFromToIndex(from, to, a.length);
        int n = to - from;
        if (n < RADIX_THRESHOLD) {
            Arrays.sort(a, from, to);
            return;
        }
        long[] src = Arrays.copyOfRange(a, from, to);
        long[] dst = new long[n];
        int[][] counts = new int[8][256];
        for (long v : src) {
            for (int pass = 0; pass < 7; pass++) counts[pass][(int) (v >>> (pass << 3)) & 0xFF]++;
            counts[7][(int) (v >>> 56) ^ 0x80]++;
        }
        for (int pass = 0; pass < 8; pass++) {
            int[] c = counts[pass];
            int shift = pass << 3;
            int flip = pass == 7 ? 0x80 : 0;
            if (c[((int) (src[0] >>> shift) & 0xFF) ^ flip] == n) continue;
            int sum = 0;
            for (int i = 0; i < 256; i++) { int t = c[i]; c[i] = sum; sum += t; }
            for (long v : src) dst[c[((int) (v >>> shift) & 0xFF) ^ flip]++] = v;
            long[] t = src; src = dst; dst = t;
        }
        System.arraycopy(src, 0, a, from, n);
    }
}
