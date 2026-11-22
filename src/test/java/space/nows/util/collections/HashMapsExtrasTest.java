package space.nows.util.collections;

import org.junit.jupiter.api.Test;
import space.nows.util.error.CapacityExceededException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HashMapsExtrasTest {
    // ---------------------------------------------------------------- construction guards
    @Test void invalidConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new IntIntMap(-1));
        assertThrows(IllegalArgumentException.class, () -> new IntIntMap(4, 0f, 0));
        assertThrows(IllegalArgumentException.class, () -> new IntIntMap(4, 1f, 0));
        assertThrows(IllegalArgumentException.class, () -> new IntObjectMap<String>(4, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new LongHashMap<String>(-5));
        assertThrows(IllegalArgumentException.class, () -> new IntHashSet(4, 2f));
        CapacityExceededException e = assertThrows(CapacityExceededException.class, () -> new IntIntMap(Integer.MAX_VALUE));
        assertEquals(1 << 30, e.limit());
        assertThrows(CapacityExceededException.class, () -> Hash.grow(1 << 30));
        assertEquals(8, Hash.grow(4));
    }

    @Test void nullActionsAndKeysAreRejected() {
        assertThrows(NullPointerException.class, () -> new IntIntMap().forEach(null));
        assertThrows(NullPointerException.class, () -> new IntHashSet().forEach(null));
        assertThrows(NullPointerException.class, () -> new ObjectIntMap<String>().put(null, 1));
        assertThrows(NullPointerException.class, () -> IntHashSet.of((int[]) null));
        assertThrows(NullPointerException.class, () -> new IntIntMap().merge(1, 1, null));
    }

    // ---------------------------------------------------------------- randomized differential tests (tiny key range = many collisions)
    @Test void intIntMapDifferential() {
        Random r = new Random(1);
        IntIntMap m = new IntIntMap();
        Map<Integer, Integer> ref = new HashMap<>();
        for (int i = 0; i < 200_000; i++) {
            int key = r.nextInt(64) - 8;               // includes 0 and negative keys
            switch (r.nextInt(5)) {
                case 0, 1 -> { int v = r.nextInt(); Integer old = ref.put(key, v); assertEquals(old == null ? 0 : old, m.put(key, v)); }
                case 2 -> { Integer old = ref.remove(key); assertEquals(old == null ? 0 : old, m.remove(key)); }
                case 3 -> { int d = r.nextInt(10); ref.merge(key, d, Integer::sum); m.addTo(key, d); }
                default -> assertEquals(ref.containsKey(key), m.containsKey(key));
            }
            if (i % 4096 == 0) assertEquals(ref.size(), m.size());
        }
        assertEquals(ref, m.toJavaMap());
        assertEquals(ref.hashCode(), m.hashCode());
        assertEquals(ref.size(), m.size());
        assertEquals(ref.size(), m.keys().length);
        assertEquals(ref.size(), m.values().length);
    }

    @Test void intObjectMapDifferential() {
        Random r = new Random(2);
        IntObjectMap<String> m = new IntObjectMap<>();
        Map<Integer, String> ref = new HashMap<>();
        for (int i = 0; i < 200_000; i++) {
            int key = r.nextInt(64) - 8;
            switch (r.nextInt(4)) {
                case 0, 1 -> { String v = "v" + r.nextInt(100); assertEquals(ref.put(key, v), m.put(key, v)); }
                case 2 -> assertEquals(ref.remove(key), m.remove(key));
                default -> assertEquals(ref.get(key), m.get(key));
            }
        }
        assertEquals(ref, m.toJavaMap());
        assertEquals(ref.hashCode(), m.hashCode());
        assertEquals(ref.size(), m.size());
    }

    @Test void longHashMapDifferential() {
        Random r = new Random(3);
        LongHashMap<Integer> m = new LongHashMap<>();
        Map<Long, Integer> ref = new HashMap<>();
        long[] pool = {0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 1L << 32, (1L << 32) + 1, 42L, 43L, 44L, -45L};
        for (int i = 0; i < 200_000; i++) {
            long key = pool[r.nextInt(pool.length)] + (r.nextInt(8) == 0 ? r.nextInt(5) : 0);
            switch (r.nextInt(4)) {
                case 0, 1 -> { Integer v = r.nextInt(1000); assertEquals(ref.put(key, v), m.put(key, v)); }
                case 2 -> assertEquals(ref.remove(key), m.remove(key));
                default -> assertEquals(ref.get(key), m.get(key));
            }
        }
        assertEquals(ref, m.toJavaMap());
        assertEquals(ref.hashCode(), m.hashCode());
        assertEquals(ref.size(), m.keys().length);
    }

    @Test void objectIntMapDifferential() {
        Random r = new Random(4);
        ObjectIntMap<String> m = new ObjectIntMap<>();
        Map<String, Integer> ref = new HashMap<>();
        for (int i = 0; i < 100_000; i++) {
            String key = "k" + r.nextInt(48);
            switch (r.nextInt(5)) {
                case 0, 1 -> { int v = r.nextInt(); Integer old = ref.put(key, v); assertEquals(old == null ? 0 : old, m.put(key, v)); }
                case 2 -> { Integer old = ref.remove(key); assertEquals(old == null ? 0 : old, m.remove(key)); }
                case 3 -> { ref.merge(key, 3, Integer::sum); m.addTo(key, 3); }
                default -> assertEquals(ref.containsKey(key), m.containsKey(key));
            }
        }
        assertEquals(ref, m.toJavaMap());
        assertEquals(ref.hashCode(), m.hashCode());
    }

    @Test void intHashSetDifferential() {
        Random r = new Random(5);
        IntHashSet s = new IntHashSet();
        Set<Integer> ref = new HashSet<>();
        for (int i = 0; i < 200_000; i++) {
            int v = r.nextInt(80) - 10;
            switch (r.nextInt(3)) {
                case 0 -> assertEquals(ref.add(v), s.add(v));
                case 1 -> assertEquals(ref.remove(v), s.remove(v));
                default -> assertEquals(ref.contains(v), s.contains(v));
            }
        }
        assertEquals(ref, s.toJavaSet());
        assertEquals(ref.hashCode(), s.hashCode());
        assertEquals(ref.size(), s.size());
        assertEquals(ref.size(), (int) s.stream().distinct().count());
        assertEquals(ref.stream().mapToLong(Integer::longValue).sum(), s.stream().asLongStream().sum());
    }

    // ---------------------------------------------------------------- new operations
    @Test void putIfAbsentAndMerge() {
        IntIntMap m = new IntIntMap();
        assertEquals(0, m.putIfAbsent(1, 10));
        assertEquals(10, m.putIfAbsent(1, 99));
        assertEquals(10, m.get(1));
        assertEquals(5, m.merge(2, 5, Integer::sum));
        assertEquals(8, m.merge(2, 3, Integer::sum));
        assertEquals(8, m.get(2));
        assertEquals(1, m.merge(0, 1, Integer::sum));            // zero key takes the special path
        assertEquals(4, m.merge(0, 3, Integer::sum));

        ObjectIntMap<String> o = new ObjectIntMap<>();
        assertEquals(7, o.merge("a", 7, Math::max));
        assertEquals(9, o.merge("a", 9, Math::max));
        assertEquals(9, o.putIfAbsent("a", 1));
        assertEquals(0, o.putIfAbsent("b", 1));
    }

    @Test void computeIfAbsentRunsTheFunctionOnlyOnce() {
        IntObjectMap<String> m = new IntObjectMap<>();
        AtomicInteger calls = new AtomicInteger();
        assertEquals("v7", m.computeIfAbsent(7, k -> { calls.incrementAndGet(); return "v" + k; }));
        assertEquals("v7", m.computeIfAbsent(7, k -> { calls.incrementAndGet(); return "other"; }));
        assertEquals(1, calls.get());
        assertNull(m.putIfAbsent(8, "x"));
        assertEquals("x", m.putIfAbsent(8, "y"));

        LongHashMap<String> l = new LongHashMap<>();
        assertEquals("v" + Long.MAX_VALUE, l.computeIfAbsent(Long.MAX_VALUE, k -> "v" + k));
        assertEquals(1, l.size());
    }

    @Test void copiesAreIndependent() {
        IntIntMap a = new IntIntMap();
        a.put(0, 1); a.put(5, 2);
        IntIntMap b = a.copy();
        assertEquals(a, b);
        a.put(6, 3); a.remove(0);
        assertEquals(2, b.size());
        assertEquals(1, b.get(0));
        assertFalse(b.containsKey(6));

        IntHashSet s = IntHashSet.of(0, 1, 2), t = s.copy();
        s.add(3);
        assertFalse(t.contains(3));
        assertEquals(3, t.size());

        IntObjectMap<String> o = new IntObjectMap<>();
        o.put(1, "x");
        IntObjectMap<String> p = o.copy();
        o.put(2, "y");
        assertEquals(1, p.size());

        LongHashMap<String> l = new LongHashMap<>();
        l.put(1L, "x");
        assertEquals(l, l.copy());
        ObjectIntMap<String> oi = new ObjectIntMap<>();
        oi.put("a", 1);
        ObjectIntMap<String> oc = oi.copy();
        oi.put("b", 2);
        assertEquals(1, oc.size());
    }

    @Test void keysAndValuesLineUp() {
        IntIntMap z = new IntIntMap();
        z.put(0, 7); z.put(5, 9);
        assertArrayEquals(new int[]{0, 5}, z.keys());
        assertArrayEquals(new int[]{7, 9}, z.values());
        IntObjectMap<String> o = new IntObjectMap<>();
        o.put(0, "z"); o.put(3, "t");
        assertArrayEquals(new int[]{0, 3}, o.keys());
        LongHashMap<String> l = new LongHashMap<>();
        l.put(0L, "z"); l.put(1L << 40, "t");
        assertArrayEquals(new long[]{0L, 1L << 40}, l.keys());
    }

    @Test void equalsAndHashCodeFollowTheJdkContract() {
        IntIntMap a = new IntIntMap(), b = new IntIntMap(1000);
        for (int i = -5; i < 40; i++) { a.put(i, i * 2); b.put(39 - (i + 5) - 5 + 5 - 5 + 0, 0); }
        b.clear();
        for (int i = 39; i >= -5; i--) b.put(i, i * 2);        // same content, different insertion order and capacity
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.put(1, -1);
        assertNotEquals(a, b);
        assertNotEquals(a, "not a map");
        assertEquals(a.toJavaMap(), Map.copyOf(a.toJavaMap()));
    }

    @Test void toStringIsReadable() {
        IntIntMap m = new IntIntMap();
        assertEquals("{}", m.toString());
        m.put(1, 2);
        assertEquals("{1=2}", m.toString());
        assertEquals("[1]", IntHashSet.of(1).toString());
        assertEquals("[]", new IntHashSet().toString());
        ObjectIntMap<String> o = new ObjectIntMap<>();
        o.put("a", 1);
        assertEquals("{a=1}", o.toString());
    }

    // ---------------------------------------------------------------- fail-fast behaviour
    @Test void structuralChangeDuringForEachFailsFast() {
        IntIntMap m = new IntIntMap();
        for (int i = 1; i <= 10; i++) m.put(i, i);
        assertThrows(ConcurrentModificationException.class, () -> m.forEach((k, v) -> m.put(k + 100, 1)));

        IntIntMap one = new IntIntMap();
        one.put(1, 1);
        assertThrows(ConcurrentModificationException.class, () -> one.forEach((k, v) -> one.remove(k)));   // even for the last element

        IntIntMap zero = new IntIntMap();
        zero.put(0, 1);
        assertThrows(ConcurrentModificationException.class, () -> zero.forEach((k, v) -> zero.clear()));

        IntObjectMap<String> o = new IntObjectMap<>();
        for (int i = 1; i <= 5; i++) o.put(i, "x");
        assertThrows(ConcurrentModificationException.class, () -> o.forEach((k, v) -> o.put(k + 50, "y")));

        LongHashMap<String> l = new LongHashMap<>();
        for (long i = 1; i <= 5; i++) l.put(i, "x");
        assertThrows(ConcurrentModificationException.class, () -> l.forEach((k, v) -> l.remove(k)));

        ObjectIntMap<String> oi = new ObjectIntMap<>();
        oi.put("a", 1); oi.put("b", 2);
        assertThrows(ConcurrentModificationException.class, () -> oi.forEach((k, v) -> oi.put(k + "!", 1)));

        IntHashSet s = IntHashSet.of(1, 2, 3);
        assertThrows(ConcurrentModificationException.class, () -> s.forEach(x -> s.add(x + 100)));
    }

    @Test void updatingAnExistingKeyIsNotAStructuralChange() {
        IntIntMap m = new IntIntMap();
        for (int i = 0; i < 20; i++) m.put(i, i);
        m.forEach((k, v) -> m.put(k, v + 1));
        for (int i = 0; i < 20; i++) assertEquals(i + 1, m.get(i));
        m.forEach((k, v) -> m.addTo(k, 1));
        assertEquals(2, m.get(0));
    }

    @Test void iteratorVisitsEveryElementOnceAndFailsFast() {
        IntHashSet s = IntHashSet.of(0, 5, -3, 77, 1000);
        List<Integer> seen = new ArrayList<>();
        PrimitiveIterator.OfInt it = s.iterator();
        while (it.hasNext()) seen.add(it.nextInt());
        assertEquals(5, seen.size());
        assertEquals(new HashSet<>(Arrays.asList(0, 5, -3, 77, 1000)), new HashSet<>(seen));
        assertThrows(NoSuchElementException.class, it::nextInt);
        assertThrows(UnsupportedOperationException.class, it::remove);

        PrimitiveIterator.OfInt stale = s.iterator();
        s.add(99999);
        assertThrows(ConcurrentModificationException.class, stale::nextInt);

        assertFalse(new IntHashSet().iterator().hasNext());
    }

    @Test void setBulkOperations() {
        IntHashSet s = new IntHashSet();
        assertTrue(s.addAll(new int[]{1, 2, 3, 0}));
        assertFalse(s.addAll(new int[]{1, 2}));
        assertTrue(s.containsAll(new int[]{0, 3}));
        assertFalse(s.containsAll(new int[]{0, 4}));
        assertEquals(IntHashSet.of(0, 1, 2, 3), s);
        assertNotEquals(IntHashSet.of(0, 1, 2), s);
        assertEquals(0, new IntHashSet().toArray().length);
    }

    @Test void growthKeepsEverything() {
        IntIntMap m = new IntIntMap(0);
        for (int i = -50_000; i < 50_000; i++) m.put(i, i ^ 0x55);
        assertEquals(100_000, m.size());
        for (int i = -50_000; i < 50_000; i++) assertEquals(i ^ 0x55, m.get(i));
        for (int i = -50_000; i < 50_000; i += 2) m.remove(i);
        assertEquals(50_000, m.size());
        for (int i = -50_000; i < 50_000; i++) assertEquals((i & 1) == 0 ? 0 : (i ^ 0x55), m.get(i));
    }
}
