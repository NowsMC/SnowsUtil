package space.nows.util.collections;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmallArrayMapTest {
    @Test void removeAndGrowth() {
        SmallArrayMap<String, Integer> c = new SmallArrayMap<>();
        for (int i = 0; i < 20; i++) c.put("k" + i, i);
        assertEquals(20, c.size());
        assertEquals(5, c.remove("k5"));
        assertNull(c.get("k5"));
        assertEquals(19, c.size());
    }

    @Test void helpers() {
        SmallArrayMap<String, Integer> m = new SmallArrayMap<>();
        assertNull(m.putIfAbsent("a", 1));
        assertEquals(1, m.putIfAbsent("a", 2));
        assertEquals(5, m.computeIfAbsent("hello", String::length));
        assertEquals(5, m.computeIfAbsent("hello", k -> -1));
        SmallArrayMap<String, Integer> c = m.copy();
        m.put("z", 9);
        assertEquals(2, c.size());
        assertEquals(Map.of("a", 1, "hello", 5), c.toJavaMap());
        assertThrows(IllegalArgumentException.class, () -> new SmallArrayMap<String, Integer>(-1));
        assertEquals("{}", new SmallArrayMap<String, Integer>().toString());
    }
}
