package space.nows.util.array;

import org.junit.jupiter.api.Test;
import space.nows.util.error.CapacityExceededException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ArrayGuardsTest {
    @Test void intArraysMatchArrayListUnderRandomEdits() {
        Random r = new Random(11);
        int[] a = new int[0];
        List<Integer> ref = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            switch (r.nextInt(4)) {
                case 0 -> { int idx = r.nextInt(ref.size() + 1), v = r.nextInt(100); a = ArrayEdit.insert(a, idx, v); ref.add(idx, v); }
                case 1 -> { if (!ref.isEmpty()) { int idx = r.nextInt(ref.size()); a = ArrayEdit.remove(a, idx); ref.remove(idx); } }
                case 2 -> { int v = r.nextInt(100); a = ArrayEdit.append(a, v); ref.add(v); }
                default -> { int v = r.nextInt(100); assertEquals(ref.indexOf(v), ArraySearch.indexOf(a, v)); assertEquals(ref.lastIndexOf(v), ArraySearch.lastIndexOf(a, v)); }
            }
        }
        assertArrayEquals(ref.stream().mapToInt(Integer::intValue).toArray(), a);
    }

    @Test void intArrayBasics() {
        int[] a = {1, 2, 3, 4};
        int[] c = ArrayCopy.copy(a);
        c[0] = 99;
        assertEquals(1, a[0]);
        assertArrayEquals(new int[]{3, 4}, ArrayEdit.concat(new int[]{3}, new int[]{4}));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, ArrayEdit.append(a, 5));
        assertEquals(3, ArraySearch.indexOf(a, 4, -10));
        assertEquals(-1, ArraySearch.indexOf(a, 1, 1));
        assertTrue(ArraySearch.contains(a, 3));
        assertEquals(1, ArrayOrder.min(a));
        assertEquals(4, ArrayOrder.max(a));
        assertTrue(ArrayOrder.isSorted(a));
        assertFalse(ArrayOrder.isSorted(new int[]{2, 1}));
        int[] r = {1, 2, 3, 4, 5};
        ArrayReorder.reverse(r, 1, 4);
        assertArrayEquals(new int[]{1, 4, 3, 2, 5}, r);
        ArrayReorder.reverse(r);
        assertArrayEquals(new int[]{5, 2, 3, 4, 1}, r);
        int[] s = {10, 20, 30, 40};
        assertEquals(3, ArrayEdit.swapRemove(s, 1, 4));
        assertArrayEquals(new int[]{10, 40, 30, 40}, s);
    }

    @Test void guardsReportTheProblem() {
        int[] a = {1, 2, 3};
        assertThrows(IllegalArgumentException.class, () -> ArrayResize.resize(a, -1));
        assertThrows(IllegalArgumentException.class, () -> ArrayResize.ensureCapacity(a, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.remove(a, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.remove(new int[0], 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.insert(a, 4, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.insert(a, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.swapRemove(a, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.swapRemove(a, 2, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayReorder.reverse(a, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayCopy.copy(a, Integer.MAX_VALUE, new int[2], 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayCopy.copy(a, 0, new int[2], 0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayCopy.copy(a, 0, new int[3], 0, -1));
        assertThrows(NullPointerException.class, () -> ArrayCopy.copy((int[]) null));
        assertThrows(NullPointerException.class, () -> ArrayEdit.append((int[]) null, 1));
        assertThrows(NoSuchElementException.class, () -> ArrayOrder.min(new int[0]));
        assertThrows(NoSuchElementException.class, () -> ArrayOrder.max(new long[0]));
    }

    @Test void ensureCapacityNeverReturnsSomethingSmallerThanAsked() {
        int[] a = {1, 2, 3};
        assertSame(a, ArrayResize.ensureCapacity(a, 3));
        int[] grown = ArrayResize.ensureCapacity(a, 4);
        assertTrue(grown.length >= 4);
        assertArrayEquals(a, java.util.Arrays.copyOf(grown, 3));
        CapacityExceededException e = assertThrows(CapacityExceededException.class, () -> ArrayResize.ensureCapacity(a, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, e.requested());
        assertThrows(CapacityExceededException.class, () -> ArrayResize.ensureCapacity(new String[0], Integer.MAX_VALUE - 7));
        assertThrows(CapacityExceededException.class, () -> ArrayResize.ensureCapacity(new byte[0], Integer.MAX_VALUE - 7));
    }

    @Test void longArraysHaveTheSameToolbox() {
        long[] a = {5L, 6L, 7L};
        assertArrayEquals(new long[]{5L, 7L}, ArrayEdit.remove(a, 1));
        assertArrayEquals(new long[]{5L, 9L, 6L, 7L}, ArrayEdit.insert(a, 1, 9L));
        assertEquals(2, ArraySearch.indexOf(a, 7L));
        assertEquals(1L << 40, ArrayOrder.max(new long[]{1L, 1L << 40, -3L}));
        assertEquals(-3L, ArrayOrder.min(new long[]{1L, 1L << 40, -3L}));
        assertTrue(ArraySearch.contains(a, 6L));
        assertEquals(0, new long[0].length);
    }

    @Test void byteArraysAreUnsignedAwareWhereItMatters() {
        assertTrue(ArrayOrder.compareUnsigned(new byte[]{(byte) 0xFF}, new byte[]{0x01}) > 0);
        assertEquals(-1, ArrayOrder.min(new byte[]{1, -1, 5}));
        assertEquals(5, ArrayOrder.max(new byte[]{1, -1, 5}));
        byte[] hay = {1, 2, 3, 2, 3, 4};
        assertEquals(1, ArraySearch.indexOf(hay, new byte[]{2, 3}));
        assertEquals(3, ArraySearch.indexOf(hay, new byte[]{2, 3}, 2));
        assertEquals(-1, ArraySearch.indexOf(hay, new byte[]{9}));
        assertEquals(2, ArraySearch.indexOf(hay, new byte[0], 2));
        assertTrue(ArraySearch.contains(hay, new byte[]{3, 4}));
        assertTrue(java.util.Arrays.equals(new byte[]{1}, new byte[]{1}));
        assertEquals(3, ArraySearch.lastIndexOf(hay, (byte) 2));
    }

    @Test void objectArraysPreserveTheComponentType() {
        String[] s = ArrayEdit.insert(new String[]{"a", "c"}, 1, "b");
        assertArrayEquals(new Object[]{"a", "b", "c"}, s);
        assertEquals(String[].class, s.getClass());
        assertEquals(String[].class, ArrayEdit.remove(s, 0).getClass());
        assertEquals(String[].class, ArrayEdit.append(s, "d").getClass());
        assertEquals(String[].class, ArrayEdit.concat(s, new String[]{"x"}).getClass());
        assertArrayEquals(new Object[]{"b", "c"}, ArrayEdit.remove(s, 0));
        assertEquals(-1, ArraySearch.indexOf(s, "zzz"));
        assertEquals(0, ArraySearch.indexOf(new String[]{null, "x"}, null));
        assertEquals(2, ArraySearch.lastIndexOf(new String[]{"a", null, null}, null));
        assertTrue(ArraySearch.contains(s, "c"));

        String[] t = {"p", "q", "r"};
        assertEquals(2, ArrayEdit.swapRemove(t, 0, 3));
        assertArrayEquals(new Object[]{"r", "q", null}, t);
        ArrayReorder.reverse(t);
        assertArrayEquals(new Object[]{null, "q", "r"}, t);
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.remove(new String[0], 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayEdit.insert(new String[0], 1, "x"));
    }
}
