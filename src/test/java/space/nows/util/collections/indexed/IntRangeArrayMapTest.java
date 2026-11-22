package space.nows.util.collections.indexed;

import org.junit.jupiter.api.Test;
import space.nows.util.error.KeyOutOfRangeException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntRangeArrayMapTest {
    @Test void rejectsOutOfRangeKeysWithDetail() {
        IntRangeArrayMap<String> m = new IntRangeArrayMap<>(10, 20);
        KeyOutOfRangeException e = assertThrows(KeyOutOfRangeException.class, () -> m.put(21, "x"));
        assertEquals(21, e.key());
        assertEquals(10, e.min());
        assertEquals(20, e.max());
        assertThrows(IndexOutOfBoundsException.class, () -> m.put(9, "x"));
        assertNull(m.get(9));
        assertNull(m.remove(-1));
        assertFalse(m.containsKey(21));
        assertEquals(0, m.size());
        assertEquals(10, m.minKey());
        assertEquals(20, m.maxKey());
    }

    @Test void rangeValidationAndExtremes() {
        assertThrows(IllegalArgumentException.class, () -> new IntRangeArrayMap<String>(5, 4));
        assertThrows(IllegalArgumentException.class, () -> new IntRangeArrayMap<String>(Integer.MIN_VALUE, Integer.MAX_VALUE));
        IntRangeArrayMap<String> neg = new IntRangeArrayMap<>(-3, 3);
        neg.put(-3, "lo");
        neg.put(3, "hi");
        assertEquals("lo", neg.get(-3));
        assertEquals("hi", neg.get(3));
        assertEquals(3, neg.maxKey());
    }

    @Test void exportsSorted() {
        IntRangeArrayMap<String> m = new IntRangeArrayMap<>(0, 10);
        m.put(5, "e"); m.put(3, "c");
        assertEquals(List.of(3, 5), new ArrayList<>(m.toJavaMap().keySet()));
        assertEquals("{3=c, 5=e}", m.toString());
        assertEquals("{}", new IntRangeArrayMap<String>(0, 1).toString());
    }

    @Test void basicOperations() {
        IntRangeArrayMap<String> f = new IntRangeArrayMap<>(-5, 5);
        f.put(-5, "min"); f.put(5, "max");
        assertEquals("min", f.get(-5));
        assertNull(f.get(100));
        assertThrows(IndexOutOfBoundsException.class, () -> f.put(6, "x"));
    }
}
