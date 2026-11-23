package space.nows.util.array;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.Set;
import org.junit.jupiter.api.Test;
import space.nows.util.array.ArrayEdit;
import space.nows.util.array.ArraySearch;
import space.nows.util.array.ArraySort;
import space.nows.util.collections.IntArrayDeque;
import space.nows.util.collections.IntHashSet;
import space.nows.util.collections.IntIntMap;
import space.nows.util.collections.IntList;
import space.nows.util.collections.IntMinHeap;
import space.nows.util.math.Modulo;
import space.nows.util.math.PowersOfTwo;
import space.nows.util.math.Rounding;
import space.nows.util.random.FastRandom;
import space.nows.util.text.Hex;
import space.nows.util.text.Padding;
import space.nows.util.text.Strings;
import space.nows.util.text.StringSplit;
import static org.junit.jupiter.api.Assertions.*;

class ArrayDifferentialTest {
    private static final int[] EXTREMES = {0, 1, -1, 2, -2, 127, 128, -128, 255, 256, 65535, 65536,
            Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1};

    private static List<Integer> toList(int[] a) {
        List<Integer> out = new ArrayList<>();
        for (int v : a) out.add(v);
        return out;
    }


    @Test
    void radixSortMatchesArraysSort() {
        Random r = new Random(6);
        for (int trial = 0; trial < 60; trial++) {
            int n = r.nextInt(5000);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = r.nextInt(4) == 0 ? EXTREMES[r.nextInt(EXTREMES.length)] : r.nextInt();
            int[] expected = a.clone();
            Arrays.sort(expected);
            ArraySort.radixSort(a);
            assertArrayEquals(expected, a, "n=" + n);
        }
        int[] range = {9, 8, 7, 6, 5};
        int[] big = new int[3000];
        for (int i = 0; i < big.length; i++) big[i] = -i;
        int[] expected = big.clone();
        Arrays.sort(expected, 100, 2500);
        ArraySort.radixSort(big, 100, 2500);
        assertArrayEquals(expected, big, "sub-range");
        assertArrayEquals(new int[]{9, 8, 7, 6, 5}, range);
    }

    @Test
    void radixSortLongMatchesArraysSort() {
        Random r = new Random(7);
        for (int trial = 0; trial < 40; trial++) {
            long[] a = new long[r.nextInt(4000)];
            for (int i = 0; i < a.length; i++) a[i] = r.nextBoolean() ? r.nextLong() : (r.nextBoolean() ? Long.MIN_VALUE : Long.MAX_VALUE) - r.nextInt(3);
            long[] expected = a.clone();
            Arrays.sort(expected);
            ArraySort.radixSort(a);
            assertArrayEquals(expected, a);
        }
    }

    @Test
    void byteSearchMatchesNaive() {
        Random r = new Random(8);
        for (int trial = 0; trial < 2000; trial++) {
            byte[] a = new byte[r.nextInt(40)];
            r.nextBytes(a);
            byte v = a.length > 0 && r.nextBoolean() ? a[r.nextInt(a.length)] : (byte) r.nextInt();
            int from = r.nextInt(a.length + 3) - 2;
            int expected = -1;
            for (int i = Math.max(from, 0); i < a.length; i++) if (a[i] == v) { expected = i; break; }
            assertEquals(expected, ArraySearch.indexOf(a, v, from), "from=" + from + " v=" + v + " a=" + Arrays.toString(a));
        }
    }

    @Test
    void arrayEditMatchesLists() {
        Random r = new Random(9);
        for (int trial = 0; trial < 500; trial++) {
            int[] a = new int[r.nextInt(10)];
            for (int i = 0; i < a.length; i++) a[i] = r.nextInt(100);
            int i = r.nextInt(a.length + 1);
            int v = r.nextInt(100);
            List<Integer> m = toList(a);
            m.add(i, v);
            assertEquals(m, toList(ArrayEdit.insert(a, i, v)), "insert");
            if (a.length > 0) {
                int j = r.nextInt(a.length);
                List<Integer> removed = new ArrayList<>(toList(a));
                removed.remove(j);
                assertEquals(removed, toList(ArrayEdit.remove(a, j)), "remove");
            }
        }
    }

}
