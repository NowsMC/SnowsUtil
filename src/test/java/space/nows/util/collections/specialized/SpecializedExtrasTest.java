package space.nows.util.collections.specialized;

import org.junit.jupiter.api.Test;
import space.nows.util.error.KeyOutOfRangeException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpecializedExtrasTest {
    enum Color { RED, GREEN, BLUE }

    @Test void fixedArrayMapRejectsOutOfRangeKeysWithDetail() {
        FixedArrayMap<String> m = new FixedArrayMap<>(10, 20);
        KeyOutOfRangeException e = assertThrows(KeyOutOfRangeException.class, () -> m.put(21, "x"));
        assertEquals(21, e.key());
        assertEquals(10, e.min());
        assertEquals(20, e.max());
        assertThrows(IndexOutOfBoundsException.class, () -> m.put(9, "x"));
        // lookups follow Map semantics: an out-of-range key is simply absent
        assertNull(m.get(9));
        assertNull(m.remove(-1));
        assertFalse(m.containsKey(21));
        assertEquals(0, m.size());
        assertEquals(10, m.minKey());
        assertEquals(20, m.maxKey());
    }

    @Test void fixedArrayMapRangeValidationAndExtremes() {
        assertThrows(IllegalArgumentException.class, () -> new FixedArrayMap<String>(5, 4));
        assertThrows(IllegalArgumentException.class, () -> new FixedArrayMap<String>(Integer.MIN_VALUE, Integer.MAX_VALUE));
        FixedArrayMap<String> neg = new FixedArrayMap<>(-3, 3);
        neg.put(-3, "lo");
        neg.put(3, "hi");
        assertEquals("lo", neg.get(-3));
        assertEquals("hi", neg.get(3));
        assertEquals(3, neg.maxKey());
    }

    @Test void fixedArrayMapExportsSorted() {
        FixedArrayMap<String> m = new FixedArrayMap<>(0, 10);
        m.put(5, "e"); m.put(3, "c");
        assertEquals(List.of(3, 5), new ArrayList<>(m.toJavaMap().keySet()));
        assertEquals("{3=c, 5=e}", m.toString());
        assertEquals("{}", new FixedArrayMap<String>(0, 1).toString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test void enumMapBridgesToTheJdkAndCopiesIndependently() {
        FastEnumMap<Color, Integer> m = new FastEnumMap<>(Color.class);
        m.put(Color.BLUE, 3);
        m.put(Color.RED, 1);
        EnumMap<Color, Integer> jdk = m.toEnumMap();
        assertEquals(Map.of(Color.RED, 1, Color.BLUE, 3), jdk);
        FastEnumMap<Color, Integer> c = m.copy();
        m.put(Color.GREEN, 2);
        assertEquals(2, c.size());
        assertEquals("{RED=1, BLUE=3}", c.toString());
        assertThrows(NullPointerException.class, () -> new FastEnumMap<Color, Integer>(null));
        assertThrows(IllegalArgumentException.class, () -> new FastEnumMap(String.class));
    }

    @Test void compactMapHelpers() {
        CompactHashMap<String, Integer> m = new CompactHashMap<>();
        assertNull(m.putIfAbsent("a", 1));
        assertEquals(1, m.putIfAbsent("a", 2));
        assertEquals(5, m.computeIfAbsent("hello", String::length));
        assertEquals(5, m.computeIfAbsent("hello", k -> -1));
        CompactHashMap<String, Integer> c = m.copy();
        m.put("z", 9);
        assertEquals(2, c.size());
        assertEquals(Map.of("a", 1, "hello", 5), c.toJavaMap());
        assertThrows(IllegalArgumentException.class, () -> new CompactHashMap<String, Integer>(-1));
        assertEquals("{}", new CompactHashMap<String, Integer>().toString());
    }
}
