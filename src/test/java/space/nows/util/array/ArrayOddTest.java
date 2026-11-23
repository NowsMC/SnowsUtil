package space.nows.util.array;

import org.junit.jupiter.api.Test;
import space.nows.util.io.ByteAccess;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class ArrayOddTest {
    @Test
    void emptyArraysBehave() {
        assertTrue(ArrayOrder.isSorted(new int[0]));
        assertTrue(ArrayOrder.isSorted(new byte[0]));
        assertThrows(RuntimeException.class, () -> ArrayOrder.min(new int[0]));
        assertThrows(RuntimeException.class, () -> ArrayOrder.max(new long[0]));
        assertEquals(0L, ArrayMath.sum(new int[0]));
        assertEquals(0, ArrayMath.prefixSum(new int[0]).length);
        assertEquals(0, ArrayCapacity.ensureCapacity(new int[0], 0).length);
    }

    @Test
    void minMaxAndSortednessMatchReference() {
        Random r = new Random(101);
        for (int trial = 0; trial < 5000; trial++) {
            int n = 1 + r.nextInt(12);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = r.nextInt(20) - 10;
            assertEquals(Arrays.stream(a).min().getAsInt(), ArrayOrder.min(a));
            assertEquals(Arrays.stream(a).max().getAsInt(), ArrayOrder.max(a));
            int[] sorted = a.clone();
            Arrays.sort(sorted);
            assertTrue(ArrayOrder.isSorted(sorted));
            assertEquals(Arrays.equals(a, sorted), ArrayOrder.isSorted(a));
        }
    }

    @Test
    void sumUsesLongAndPrefixSumWrapsOnlyInsideInt() {
        int[] big = {Integer.MAX_VALUE, Integer.MAX_VALUE, 1};
        assertEquals(2L * Integer.MAX_VALUE + 1, ArrayMath.sum(big), "sum must not overflow int");
        long[] prefix = ArrayMath.prefixSum(new long[]{Long.MAX_VALUE, 1});
        assertEquals(Long.MIN_VALUE, prefix[1], "long prefix sums wrap like long arithmetic");
        int[] p = ArrayMath.prefixSum(new int[]{1, 2, 3});
        assertArrayEquals(new int[]{1, 3, 6}, p);
    }

    @Test
    void reverseAndSwapRangesMatchReference() {
        Random r = new Random(102);
        for (int trial = 0; trial < 2000; trial++) {
            int n = r.nextInt(20);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = i;
            int from = r.nextInt(n + 1);
            int to = from + r.nextInt(n - from + 1);
            int[] expected = a.clone();
            for (int i = from, j = to - 1; i < j; i++, j--) {
                int t = expected[i];
                expected[i] = expected[j];
                expected[j] = t;
            }
            ArrayReorder.reverse(a, from, to);
            assertArrayEquals(expected, a);
        }
        assertThrows(RuntimeException.class, () -> ArrayReorder.reverse(new int[3], 2, 1));
        assertThrows(RuntimeException.class, () -> ArrayReorder.reverse(new int[3], 0, 4));
    }

    @Test
    void shuffleIsAPermutationAndFairEnoughForTwoElements() {
        Random r = new Random(103);
        int[] counts = new int[2];
        for (int trial = 0; trial < 20_000; trial++) {
            int[] a = {0, 1};
            ArrayReorder.shuffle(a, r);
            assertTrue(Arrays.equals(new int[]{0, 1}, a) || Arrays.equals(new int[]{1, 0}, a));
            counts[a[0]]++;
        }
        assertTrue(Math.abs(counts[0] - counts[1]) < 600, "two-element shuffle should be near 50/50");
        int[] single = {9};
        ArrayReorder.shuffle(single);
        assertEquals(9, single[0]);
        ArrayReorder.shuffle(new int[0], ThreadLocalRandom.current());
    }

    @Test
    void ensureCapacityKeepsContentAndRejectsNegative() {
        int[] a = {1, 2};
        int[] grown = ArrayCapacity.ensureCapacity(a, 10);
        assertTrue(grown.length >= 10);
        assertEquals(1, grown[0]);
        assertEquals(2, grown[1]);
        assertSame(a, ArrayCapacity.ensureCapacity(a, 1), "no copy when capacity suffices");
        assertThrows(IllegalArgumentException.class, () -> ArrayCapacity.ensureCapacity(a, -1));
    }

    @Test
    void editRemoveInsertAppendAndConcatAtEveryPosition() {
        int[] base = {10, 20, 30};
        for (int i = 0; i <= base.length; i++) {
            int[] inserted = ArrayEdit.insert(base, i, 99);
            assertEquals(base.length + 1, inserted.length);
            assertEquals(99, inserted[i]);
        }
        assertArrayEquals(new int[]{10, 20, 30, 40}, ArrayEdit.append(base, 40));
        assertArrayEquals(new int[]{10, 20, 30, 10, 20, 30}, ArrayEdit.concat(base, base));
        assertArrayEquals(new int[]{}, ArrayEdit.concat(new int[0], new int[0]));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.insert(base, 4, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.remove(new int[0], 0));
    }

    @Test
    void swapRemoveMovesLastIntoPlace() {
        int[] a = {1, 2, 3, 4};
        int size = ArrayEdit.swapRemove(a, 0, 4);
        assertEquals(3, size);
        assertEquals(4, a[0]);
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.swapRemove(a, 3, 3));
    }

    @Test
    void byteArraySearchMatchesBitSetOnSparseData() {
        Random r = new Random(104);
        for (int trial = 0; trial < 500; trial++) {
            byte[] data = new byte[64 + r.nextInt(64)];
            BitSet marks = new BitSet();
            for (int i = 0; i < data.length; i++) if (r.nextInt(40) == 0) { data[i] = 7; marks.set(i); }
            assertEquals(marks.nextSetBit(0), ArraySearch.indexOf(data, (byte) 7));
        }
    }

    @Test
    void byteAccessRejectsShortBuffers() {
        byte[] small = new byte[3];
        assertThrows(IndexOutOfBoundsException.class, () -> ByteAccess.readInt(small, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteAccess.readShortLE(small, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteAccess.writeLong(new byte[7], 0, 1L));
    }
}
