package space.nows.util.collections;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HashCollectionsTest {
    @Test void intIntMapMatchesHashMapUnderRandomOps() {
        Random rnd = new Random(42);
        IntIntMap map = new IntIntMap();
        Map<Integer, Integer> ref = new HashMap<>();
        for (int i = 0; i < 200_000; i++) {
            int key = rnd.nextInt(2000) - 1000;
            switch (rnd.nextInt(3)) {
                case 0 -> { int v = rnd.nextInt(); map.put(key, v); ref.put(key, v); }
                case 1 -> { Integer r = ref.remove(key); assertEquals(r == null ? 0 : r, map.remove(key)); }
                default -> { assertEquals(ref.containsKey(key), map.containsKey(key));
                             assertEquals(ref.getOrDefault(key, 0), map.get(key)); }
            }
            assertEquals(ref.size(), map.size());
        }
        int[] count = {0};
        map.forEach((k, v) -> { assertEquals(ref.get(k), v); count[0]++; });
        assertEquals(ref.size(), count[0]);
    }

    @Test void intIntMapAddToAndClear() {
        IntIntMap m = new IntIntMap();
        assertEquals(5, m.addTo(7, 5));
        assertEquals(8, m.addTo(7, 3));
        assertEquals(2, m.addTo(0, 2));
        assertEquals(2, m.size());
        m.clear();
        assertTrue(m.isEmpty());
        assertFalse(m.containsKey(0));
    }

    @Test void intObjectMapBasics() {
        IntObjectMap<String> m = new IntObjectMap<>();
        assertNull(m.put(0, "zero"));
        assertNull(m.put(10, "ten"));
        assertEquals("ten", m.put(10, "TEN"));
        assertEquals("zero", m.get(0));
        assertEquals("TEN", m.remove(10));
        assertNull(m.get(10));
        for (int i = 1; i <= 1000; i++) m.put(i, "v" + i);
        assertEquals(1001, m.size());
        for (int i = 1; i <= 1000; i++) assertEquals("v" + i, m.get(i));
    }

    @Test void longHashMapBasics() {
        LongHashMap<Integer> m = new LongHashMap<>();
        for (long i = -500; i < 500; i++) m.put(i * 1_000_003L, (int) i);
        assertEquals(1000, m.size());
        for (long i = -500; i < 500; i++) assertEquals((int) i, m.get(i * 1_000_003L));
        for (long i = -500; i < 500; i += 2) m.remove(i * 1_000_003L);
        assertEquals(500, m.size());
    }

    @Test void objectIntMapBasics() {
        ObjectIntMap<String> m = new ObjectIntMap<>();
        m.addTo("a", 1); m.addTo("a", 1); m.put("b", 9);
        assertEquals(2, m.get("a"));
        assertEquals(9, m.remove("b"));
        assertFalse(m.containsKey("b"));
        assertThrows(NullPointerException.class, () -> m.put(null, 1));
    }

    @Test void intHashSetMatchesHashSet() {
        Random rnd = new Random(7);
        IntHashSet s = new IntHashSet();
        Set<Integer> ref = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            int k = rnd.nextInt(500) - 250;
            if (rnd.nextBoolean()) assertEquals(ref.add(k), s.add(k));
            else assertEquals(ref.remove(k), s.remove(k));
            assertEquals(ref.size(), s.size());
        }
        for (int k = -250; k < 250; k++) assertEquals(ref.contains(k), s.contains(k));
    }
}
