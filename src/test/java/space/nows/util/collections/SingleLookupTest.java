package space.nows.util.collections;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SingleLookupTest {
    @Test void intIntMapPutIfAbsentAndMerge() {
        IntIntMap m = new IntIntMap(4, 0.6f, -1);
        assertEquals(-1, m.putIfAbsent(0, 7));
        assertEquals(7, m.putIfAbsent(0, 9));
        assertEquals(-1, m.putIfAbsent(5, 1));
        assertEquals(1, m.putIfAbsent(5, 2));
        assertEquals(8, m.merge(0, 1, Integer::sum));
        assertEquals(3, m.merge(5, 2, Integer::sum));
        assertEquals(4, m.merge(9, 4, Integer::sum));
        assertEquals(3, m.size());
    }

    @Test void objectMapsKeepPresentNullValues() {
        IntObjectMap<String> a = new IntObjectMap<>();
        a.put(0, null);
        a.put(3, null);
        assertNull(a.putIfAbsent(0, "x"));
        assertNull(a.putIfAbsent(3, "x"));
        assertNull(a.get(0));
        assertNull(a.get(3));
        AtomicInteger calls = new AtomicInteger();
        assertNull(a.computeIfAbsent(3, k -> { calls.incrementAndGet(); return "y"; }));
        assertEquals(0, calls.get());
        assertEquals("y", a.computeIfAbsent(8, k -> { calls.incrementAndGet(); return "y"; }));
        assertEquals(1, calls.get());
        assertEquals("d", a.getOrDefault(77, "d"));
        assertNull(a.getOrDefault(3, "d"));

        LongObjectMap<String> b = new LongObjectMap<>();
        b.put(0L, null);
        assertNull(b.putIfAbsent(0L, "x"));
        assertNull(b.get(0L));
        assertEquals("z", b.computeIfAbsent(1L << 40, k -> "z"));
        assertEquals("z", b.putIfAbsent(1L << 40, "w"));
    }

    @Test void objectIntMapMergeAndPutIfAbsent() {
        ObjectIntMap<String> m = new ObjectIntMap<>(4, 0.6f, -1);
        assertEquals(-1, m.putIfAbsent("a", 1));
        assertEquals(1, m.putIfAbsent("a", 2));
        assertEquals(5, m.merge("a", 4, Integer::sum));
        assertEquals(2, m.merge("b", 2, Integer::sum));
        assertThrows(NullPointerException.class, () -> m.putIfAbsent(null, 1));
        assertThrows(NullPointerException.class, () -> m.merge(null, 1, Integer::sum));
    }

    @Test void smallArrayMapSingleLookup() {
        SmallArrayMap<String, Integer> m = new SmallArrayMap<>();
        m.put(null, null);
        assertNull(m.putIfAbsent(null, 1));
        assertTrue(m.containsKey(null));
        assertEquals(5, m.computeIfAbsent("k", k -> 5));
        assertEquals(5, m.computeIfAbsent("k", k -> 6));
    }

    @Test void clearOnEmptyKeepsTablesUsable() {
        IntIntMap m = new IntIntMap();
        m.clear();
        m.put(1, 2);
        m.clear();
        assertTrue(m.isEmpty());
        assertEquals(0, m.get(1));
    }
}
