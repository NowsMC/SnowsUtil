package space.nows.util.collections;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedCollectionsTest {
    @Test void longIntMapMatchesHashMap() {
        Random r = new Random(21);
        LongIntMap map = new LongIntMap(2, 0.6f, -1);
        Map<Long, Integer> ref = new HashMap<>();
        for (int i = 0; i < 20000; i++) {
            long k = r.nextInt(300) == 0 ? 0L : (long) r.nextInt(1500) << 29;
            int v = r.nextInt();
            switch (r.nextInt(6)) {
                case 0 -> { Integer old = ref.put(k, v); assertEquals(old == null ? -1 : old, map.put(k, v)); }
                case 1 -> assertEquals(ref.merge(k, 3, Integer::sum), map.addTo(k, 3));
                case 2 -> { Integer old = ref.remove(k); assertEquals(old == null ? -1 : old, map.remove(k)); }
                case 3 -> assertEquals(ref.merge(k, v, Integer::max), map.merge(k, v, Integer::max));
                case 4 -> { Integer old = ref.putIfAbsent(k, v); assertEquals(old == null ? -1 : old, map.putIfAbsent(k, v)); }
                default -> assertEquals(ref.getOrDefault(k, -1), map.get(k));
            }
            assertEquals(ref.size(), map.size());
        }
        assertEquals(ref, map.toJavaMap());
        assertEquals(ref.hashCode(), map.hashCode());
        assertEquals(map, map.copy());
        map.trim();
        assertEquals(ref, map.toJavaMap());
        assertEquals(map.keys().length, map.values().length);
    }

    @Test void objectLongMapMatchesHashMap() {
        Random r = new Random(22);
        ObjectLongMap<String> map = new ObjectLongMap<>(2, 0.6f, -1L);
        Map<String, Long> ref = new HashMap<>();
        for (int i = 0; i < 20000; i++) {
            String k = "k" + r.nextInt(400);
            long v = r.nextLong();
            switch (r.nextInt(6)) {
                case 0 -> { Long old = ref.put(k, v); assertEquals(old == null ? -1L : old, map.put(k, v)); }
                case 1 -> assertEquals(ref.merge(k, 7L, Long::sum), map.addTo(k, 7L));
                case 2 -> { Long old = ref.remove(k); assertEquals(old == null ? -1L : old, map.remove(k)); }
                case 3 -> assertEquals(ref.merge(k, v, Long::min), map.merge(k, v, Long::min));
                case 4 -> { Long old = ref.putIfAbsent(k, v); assertEquals(old == null ? -1L : old, map.putIfAbsent(k, v)); }
                default -> assertEquals(ref.getOrDefault(k, -1L), map.get(k));
            }
            assertEquals(ref.size(), map.size());
        }
        assertEquals(ref, map.toJavaMap());
        assertEquals(ref.hashCode(), map.hashCode());
        assertEquals(map, map.copy());
        assertThrows(NullPointerException.class, () -> map.put(null, 1L));
        assertThrows(NullPointerException.class, () -> map.get(null));
        assertThrows(NullPointerException.class, () -> map.merge(null, 1L, Long::sum));
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test void intSetAlgebraMatchesHashSet() {
        Random r = new Random(23);
        for (int round = 0; round < 200; round++) {
            IntHashSet a = new IntHashSet(), b = new IntHashSet();
            Set<Integer> ra = new HashSet<>(), rb = new HashSet<>();
            for (int i = 0; i < r.nextInt(60); i++) { int v = r.nextInt(30) - 5; a.add(v); ra.add(v); }
            for (int i = 0; i < r.nextInt(60); i++) { int v = r.nextInt(30) - 5; b.add(v); rb.add(v); }
            switch (round % 3) {
                case 0 -> assertEquals(ra.addAll(rb), a.addAll(b));
                case 1 -> assertEquals(ra.removeAll(rb), a.removeAll(b));
                default -> assertEquals(ra.retainAll(rb), a.retainAll(b));
            }
            assertEquals(ra, a.toJavaSet());
            assertEquals(ra.size(), a.size());
        }
    }

    @Test void longSetAlgebraAndArrays() {
        LongHashSet a = LongHashSet.of(0L, 1L, 1L << 40, -5L);
        assertTrue(a.removeAll(new long[] {1L, 99L}));
        assertFalse(a.removeAll(new long[] {1L, 99L}));
        assertEquals(Set.of(0L, 1L << 40, -5L), a.toJavaSet());
        assertTrue(a.retainAll(LongHashSet.of(0L, -5L)));
        assertEquals(Set.of(0L, -5L), a.toJavaSet());
        assertFalse(a.retainAll(LongHashSet.of(0L, -5L, 7L)));
        assertTrue(a.addAll(LongHashSet.of(7L)));
        assertTrue(a.containsAll(new long[] {0L, -5L, 7L}));
        assertThrows(NullPointerException.class, () -> a.addAll((LongHashSet) null));
        assertThrows(NullPointerException.class, () -> a.removeAll((long[]) null));
    }

    @Test void generatedClassesShareTheSameContractForZeroKeys() {
        IntObjectMap<String> a = new IntObjectMap<>();
        LongObjectMap<String> b = new LongObjectMap<>();
        ObjectIntMap<String> c = new ObjectIntMap<>();
        a.put(0, "x");
        b.put(0L, "x");
        c.put("", 0);
        assertEquals("x", a.get(0));
        assertEquals("x", b.get(0L));
        assertTrue(c.containsKey(""));
        assertEquals("{0=x}", a.toString());
        assertEquals("{0=x}", b.toString());
        assertEquals("{=0}", c.toString());
    }
}
