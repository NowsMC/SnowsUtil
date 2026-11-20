package space.nows.util.collections.specialized;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecializedMapsTest {
    enum Color { RED, GREEN, BLUE }

    @Test void compactAndEnumAndFixedMaps() {
        CompactHashMap<String, Integer> c = new CompactHashMap<>();
        for (int i = 0; i < 20; i++) c.put("k" + i, i);
        assertEquals(20, c.size());
        assertEquals(5, c.remove("k5"));
        assertNull(c.get("k5"));
        assertEquals(19, c.size());

        FastEnumMap<Color, String> e = new FastEnumMap<>(Color.class);
        e.put(Color.GREEN, "g");
        assertTrue(e.containsKey(Color.GREEN));
        assertFalse(e.containsKey(Color.RED));
        assertEquals(1, e.size());

        FixedArrayMap<String> f = new FixedArrayMap<>(-5, 5);
        f.put(-5, "min"); f.put(5, "max");
        assertEquals("min", f.get(-5));
        assertNull(f.get(100));
        assertThrows(IndexOutOfBoundsException.class, () -> f.put(6, "x"));
    }
}
