package space.nows.util.array;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArraysTest {
    @Test void intArrays() {
        int[] a = {1, 2, 3, 4};
        assertArrayEquals(new int[]{1, 3, 4}, IntArrays.remove(a, 1));
        assertArrayEquals(new int[]{1, 9, 2, 3, 4}, IntArrays.insert(a, 1, 9));
        IntArrays.swap(a, 0, 3);
        assertArrayEquals(new int[]{4, 2, 3, 1}, a);
        assertEquals(2, IntArrays.indexOf(a, 3));
        assertEquals(3, IntArrays.swapRemove(a, 0, 4));
        assertEquals(1, a[0]);
    }

    @Test void byteArrays() {
        assertEquals(2, ByteArrays.indexOf(new byte[]{9, 8, 7, 6}, new byte[]{7, 6}));
        assertTrue(ByteArrays.compare(new byte[]{(byte) 0xFF}, new byte[]{0x01}) > 0);
    }
}
