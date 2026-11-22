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
        int[] a = IntArrays.EMPTY_INT;
        List<Integer> ref = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            switch (r.nextInt(4)) {
                case 0 -> { int idx = r.nextInt(ref.size() + 1), v = r.nextInt(100); a = IntArrays.insert(a, idx, v); ref.add(idx, v); }
                case 1 -> { if (!ref.isEmpty()) { int idx = r.nextInt(ref.size()); a = IntArrays.remove(a, idx); ref.remove(idx); } }
                case 2 -> { int v = r.nextInt(100); a = IntArrays.append(a, v); ref.add(v); }
                default -> { int v = r.nextInt(100); assertEquals(ref.indexOf(v), IntArrays.indexOf(a, v)); assertEquals(ref.lastIndexOf(v), IntArrays.lastIndexOf(a, v)); }
            }
        }
        assertArrayEquals(ref.stream().mapToInt(Integer::intValue).toArray(), a);
    }

    @Test void intArrayBasics() {
        int[] a = {1, 2, 3, 4};
        int[] c = IntArrays.copy(a);
        c[0] = 99;
        assertEquals(1, a[0]);
        assertArrayEquals(new int[]{3, 4}, IntArrays.concat(new int[]{3}, new int[]{4}));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, IntArrays.append(a, 5));
        assertEquals(3, IntArrays.indexOf(a, 4, -10));          // negative start is clamped
        assertEquals(-1, IntArrays.indexOf(a, 1, 1));
        assertTrue(IntArrays.contains(a, 3));
        assertEquals(1, IntArrays.min(a));
        assertEquals(4, IntArrays.max(a));
        assertTrue(IntArrays.isSorted(a));
        assertFalse(IntArrays.isSorted(new int[]{2, 1}));
        int[] r = {1, 2, 3, 4, 5};
        IntArrays.reverse(r, 1, 4);
        assertArrayEquals(new int[]{1, 4, 3, 2, 5}, r);
        IntArrays.reverse(r);
        assertArrayEquals(new int[]{5, 2, 3, 4, 1}, r);
        int[] s = {10, 20, 30, 40};
        assertEquals(3, IntArrays.swapRemove(s, 1, 4));
        assertArrayEquals(new int[]{10, 40, 30, 40}, s);
    }

    @Test void guardsReportTheProblem() {
        int[] a = {1, 2, 3};
        assertThrows(IllegalArgumentException.class, () -> IntArrays.resize(a, -1));
        assertThrows(IllegalArgumentException.class, () -> IntArrays.ensureCapacity(a, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.remove(a, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.remove(IntArrays.EMPTY_INT, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.insert(a, 4, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.insert(a, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.swapRemove(a, 0, 4));   // size beyond the array
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.swapRemove(a, 2, 2));   // index beyond size
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.reverse(a, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.copy(a, Integer.MAX_VALUE, new int[2], 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.copy(a, 0, new int[2], 0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> IntArrays.copy(a, 0, new int[3], 0, -1));
        assertThrows(NullPointerException.class, () -> IntArrays.copy(null));
        assertThrows(NullPointerException.class, () -> IntArrays.append(null, 1));
        assertThrows(NoSuchElementException.class, () -> IntArrays.min(new int[0]));
        assertThrows(NoSuchElementException.class, () -> LongArrays.max(new long[0]));
    }

    @Test void ensureCapacityNeverReturnsSomethingSmallerThanAsked() {
        int[] a = {1, 2, 3};
        assertSame(a, IntArrays.ensureCapacity(a, 3));
        int[] grown = IntArrays.ensureCapacity(a, 4);
        assertTrue(grown.length >= 4);
        assertArrayEquals(a, java.util.Arrays.copyOf(grown, 3));
        // formerly this silently returned an array of the maximum length, i.e. SMALLER than requested
        CapacityExceededException e = assertThrows(CapacityExceededException.class, () -> IntArrays.ensureCapacity(a, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, e.requested());
        assertThrows(CapacityExceededException.class, () -> ObjectArrays.ensureCapacity(new String[0], Integer.MAX_VALUE - 7));
        assertThrows(CapacityExceededException.class, () -> ByteArrays.ensureCapacity(new byte[0], Integer.MAX_VALUE - 7));
    }

    @Test void longArraysHaveTheSameToolbox() {
        long[] a = {5L, 6L, 7L};
        assertArrayEquals(new long[]{5L, 7L}, LongArrays.remove(a, 1));
        assertArrayEquals(new long[]{5L, 9L, 6L, 7L}, LongArrays.insert(a, 1, 9L));
        assertEquals(2, LongArrays.indexOf(a, 7L));
        assertEquals(1L << 40, LongArrays.max(new long[]{1L, 1L << 40, -3L}));
        assertEquals(-3L, LongArrays.min(new long[]{1L, 1L << 40, -3L}));
        assertTrue(LongArrays.contains(a, 6L));
        assertEquals(0, LongArrays.EMPTY_LONG.length);
    }

    @Test void byteArraysAreUnsignedAwareWhereItMatters() {
        assertTrue(ByteArrays.compare(new byte[]{(byte) 0xFF}, new byte[]{0x01}) > 0);   // unsigned comparison
        assertEquals(-1, ByteArrays.min(new byte[]{1, -1, 5}));                            // min/max are signed, like the type
        assertEquals(5, ByteArrays.max(new byte[]{1, -1, 5}));
        byte[] hay = {1, 2, 3, 2, 3, 4};
        assertEquals(1, ByteArrays.indexOf(hay, new byte[]{2, 3}));
        assertEquals(3, ByteArrays.indexOf(hay, new byte[]{2, 3}, 2));
        assertEquals(-1, ByteArrays.indexOf(hay, new byte[]{9}));
        assertEquals(2, ByteArrays.indexOf(hay, new byte[0], 2));
        assertTrue(ByteArrays.contains(hay, new byte[]{3, 4}));
        assertTrue(ByteArrays.equals(new byte[]{1}, new byte[]{1}));
        assertEquals(3, ByteArrays.lastIndexOf(hay, (byte) 2));
    }

    @Test void objectArraysPreserveTheComponentType() {
        String[] s = ObjectArrays.insert(new String[]{"a", "c"}, 1, "b");
        assertArrayEquals(new Object[]{"a", "b", "c"}, s);
        assertEquals(String[].class, s.getClass());
        assertEquals(String[].class, ObjectArrays.remove(s, 0).getClass());
        assertEquals(String[].class, ObjectArrays.append(s, "d").getClass());
        assertEquals(String[].class, ObjectArrays.concat(s, new String[]{"x"}).getClass());
        assertArrayEquals(new Object[]{"b", "c"}, ObjectArrays.remove(s, 0));
        assertEquals(-1, ObjectArrays.indexOf(s, "zzz"));
        assertEquals(0, ObjectArrays.indexOf(new String[]{null, "x"}, null));
        assertEquals(2, ObjectArrays.lastIndexOf(new String[]{"a", null, null}, null));
        assertTrue(ObjectArrays.contains(s, "c"));

        String[] t = {"p", "q", "r"};
        assertEquals(2, ObjectArrays.swapRemove(t, 0, 3));
        assertArrayEquals(new Object[]{"r", "q", null}, t);   // vacated slot is nulled so it cannot leak
        ObjectArrays.reverse(t);
        assertArrayEquals(new Object[]{null, "q", "r"}, t);
        assertThrows(IndexOutOfBoundsException.class, () -> ObjectArrays.remove(new String[0], 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ObjectArrays.insert(new String[0], 1, "x"));
    }
}
