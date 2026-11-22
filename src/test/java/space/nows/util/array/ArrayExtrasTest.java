package space.nows.util.array;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ArrayExtrasTest {
    @Test void radixSortInt() {
        Random r = new Random(1);
        for (int n : new int[] {0, 1, 2, 1023, 1024, 1025, 5000, 70000}) {
            for (int mode = 0; mode < 4; mode++) {
                int[] a = new int[n];
                for (int i = 0; i < n; i++) {
                    a[i] = switch (mode) {
                        case 0 -> r.nextInt();
                        case 1 -> r.nextInt(10) - 5;
                        case 2 -> 7;
                        default -> r.nextBoolean() ? Integer.MIN_VALUE + r.nextInt(3) : Integer.MAX_VALUE - r.nextInt(3);
                    };
                }
                int[] expected = a.clone();
                Arrays.sort(expected);
                ArraySort.radixSort(a);
                assertArrayEquals(expected, a, "n=" + n + " mode=" + mode);
            }
        }
    }

    @Test void radixSortLong() {
        Random r = new Random(2);
        for (int n : new int[] {0, 1, 1023, 1024, 4096, 50000}) {
            for (int mode = 0; mode < 4; mode++) {
                long[] a = new long[n];
                for (int i = 0; i < n; i++) {
                    a[i] = switch (mode) {
                        case 0 -> r.nextLong();
                        case 1 -> r.nextInt(10) - 5;
                        case 2 -> -1L;
                        default -> r.nextBoolean() ? Long.MIN_VALUE + r.nextInt(3) : Long.MAX_VALUE - r.nextInt(3);
                    };
                }
                long[] expected = a.clone();
                Arrays.sort(expected);
                ArraySort.radixSort(a);
                assertArrayEquals(expected, a, "n=" + n + " mode=" + mode);
            }
        }
    }

    @Test void radixSortRangeLeavesOutsideUntouched() {
        Random r = new Random(3);
        int[] a = r.ints(5000).toArray();
        int[] expected = a.clone();
        Arrays.sort(expected, 100, 4100);
        ArraySort.radixSort(a, 100, 4100);
        assertArrayEquals(expected, a);
        long[] b = r.longs(5000).toArray();
        long[] expectedLongs = b.clone();
        Arrays.sort(expectedLongs, 7, 4999);
        ArraySort.radixSort(b, 7, 4999);
        assertArrayEquals(expectedLongs, b);
        assertThrows(IndexOutOfBoundsException.class, () -> ArraySort.radixSort(new int[4], 2, 9));
        assertThrows(NullPointerException.class, () -> ArraySort.radixSort((int[]) null));
    }

    @Test void sumsAndPrefixSums() {
        assertEquals(6L, ArrayMath.sum(new int[] {1, 2, 3}));
        assertEquals(2L * Integer.MAX_VALUE, ArrayMath.sum(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE}));
        assertEquals(10L, ArrayMath.sum(new long[] {4, 6}));
        assertArrayEquals(new int[] {1, 3, 6, 10}, ArrayMath.prefixSum(new int[] {1, 2, 3, 4}));
        assertArrayEquals(new long[] {5}, ArrayMath.prefixSum(new long[] {5}));
        assertArrayEquals(new long[0], ArrayMath.prefixSum(new long[0]));
    }

    private static int naiveIndexOf(byte[] a, byte v, int from) {
        for (int i = Math.max(from, 0); i < a.length; i++) if (a[i] == v) return i;
        return -1;
    }

    @Test void byteIndexOfMatchesNaive() {
        Random r = new Random(4);
        for (int round = 0; round < 300; round++) {
            byte[] a = new byte[r.nextInt(80)];
            for (int i = 0; i < a.length; i++) a[i] = (byte) (r.nextInt(5) == 0 ? r.nextInt(256) : 0x7F);
            for (byte v : new byte[] {0x7F, 0, 1, (byte) 0x80, (byte) 0xFF, (byte) r.nextInt()}) {
                for (int from = -2; from <= a.length + 1; from++) {
                    assertEquals(naiveIndexOf(a, v, from), ArraySearch.indexOf(a, v, from));
                }
            }
        }
    }

    @Test void byteIndexOfAdjacentToMatchesDoesNotMisreport() {
        byte[] a = new byte[64];
        Arrays.fill(a, (byte) 1);
        a[40] = 2;
        assertEquals(40, ArraySearch.indexOf(a, (byte) 2));
        assertEquals(-1, ArraySearch.indexOf(a, (byte) 3));
        Arrays.fill(a, (byte) 0);
        a[63] = (byte) 0x80;
        assertEquals(63, ArraySearch.indexOf(a, (byte) 0x80));
        assertEquals(0, ArraySearch.indexOf(a, (byte) 0));
    }

    @Test void patternSearchMatchesNaive() {
        Random r = new Random(5);
        for (int round = 0; round < 500; round++) {
            byte[] data = new byte[r.nextInt(100)];
            for (int i = 0; i < data.length; i++) data[i] = (byte) r.nextInt(3);
            byte[] pattern = new byte[r.nextInt(5)];
            for (int i = 0; i < pattern.length; i++) pattern[i] = (byte) r.nextInt(3);
            for (int from = -1; from <= data.length + 1; from++) {
                int expected = -1;
                if (pattern.length == 0) {
                    expected = Math.min(Math.max(from, 0), data.length);
                } else {
                    for (int i = Math.max(from, 0); i + pattern.length <= data.length && expected < 0; i++) {
                        if (Arrays.equals(data, i, i + pattern.length, pattern, 0, pattern.length)) expected = i;
                    }
                }
                assertEquals(expected, ArraySearch.indexOf(data, pattern, from), "from=" + from);
            }
        }
    }
}
