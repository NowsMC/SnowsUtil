package space.nows.util.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class OrderedObjectMapTest {
    private static List<String> order(Map<String, ?> map) {
        return new ArrayList<>(map.keySet());
    }

    @Test
    void matchesLinkedHashMapUnderRandomOperations() {
        Random r = new Random(51);
        for (int trial = 0; trial < 300; trial++) {
            OrderedObjectMap<String, Integer> map = new OrderedObjectMap<>(r.nextInt(3));
            Map<String, Integer> ref = new LinkedHashMap<>();
            for (int op = 0; op < 600; op++) {
                String key = r.nextInt(8) == 0 ? null : "k" + r.nextInt(30);
                int value = r.nextInt(100);
                switch (r.nextInt(7)) {
                    case 0, 1 -> assertEquals(ref.put(key, value), map.put(key, value), "put " + key);
                    case 2 -> assertEquals(ref.remove(key), map.remove(key), "remove " + key);
                    case 3 -> assertEquals(ref.get(key), map.get(key), "get " + key);
                    case 4 -> assertEquals(ref.containsKey(key), map.containsKey(key), "containsKey " + key);
                    case 5 -> assertEquals(order(ref), order(map), "order at op " + op);
                    default -> {
                        if (r.nextInt(50) == 0) {
                            ref.clear();
                            map.clear();
                        }
                    }
                }
                assertEquals(ref.size(), map.size());
                assertEquals(ref.isEmpty(), map.isEmpty());
            }
            assertEquals(ref, map);
            assertEquals(ref.hashCode(), map.hashCode());
            assertEquals(order(ref), order(map));
        }
    }

    @Test
    void reinsertingARemovedKeyMovesItToTheEnd() {
        OrderedObjectMap<String, Integer> map = new OrderedObjectMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.remove("a");
        map.put("a", 4);
        assertEquals(List.of("b", "c", "a"), order(map));
        assertEquals(4, map.get("a"));
    }

    @Test
    void growsAndCompactsManyRemovals() {
        OrderedObjectMap<Integer, Integer> map = new OrderedObjectMap<>();
        for (int round = 0; round < 50; round++) {
            for (int i = 0; i < 500; i++) map.put(round * 1000 + i, i);
            for (int i = 0; i < 499; i++) map.remove(round * 1000 + i);
        }
        assertEquals(50, map.size());
        int expected = 499;
        for (Integer key : map.keySet()) {
            assertEquals(expected, key % 1000);
            expected = 499;
        }
        assertEquals(50, map.values().size());
    }

    @Test
    void iteratorRemoveAndFailFast() {
        OrderedObjectMap<String, Integer> map = new OrderedObjectMap<>();
        map.put("x", 1);
        map.put("y", 2);
        map.put("z", 3);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        it.next();
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
        assertEquals(List.of("y", "z"), order(map));
        Iterator<Map.Entry<String, Integer>> stale = map.entrySet().iterator();
        map.put("w", 4);
        assertThrows(ConcurrentModificationException.class, stale::next);
        Iterator<String> empty = new OrderedObjectMap<String, String>().keySet().iterator();
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::next);
    }

    @Test
    void entrySetValueWritesThrough() {
        OrderedObjectMap<String, Integer> map = new OrderedObjectMap<>();
        map.put("a", 1);
        for (Map.Entry<String, Integer> e : map.entrySet()) e.setValue(e.getValue() + 10);
        assertEquals(11, map.get("a"));
    }

    @Test
    void nullKeysAndValuesFollowTheMapContract() {
        OrderedObjectMap<String, String> map = new OrderedObjectMap<>();
        assertNull(map.put(null, null));
        assertTrue(map.containsKey(null));
        assertNull(map.get(null));
        assertEquals(1, map.size());
        assertNull(map.remove(null));
        assertEquals(0, map.size());
    }

    @Test
    void copyConstructorPreservesOrder() {
        LinkedHashMap<String, Integer> source = new LinkedHashMap<>();
        source.put("q", 1);
        source.put("p", 2);
        OrderedObjectMap<String, Integer> copy = new OrderedObjectMap<>(source);
        assertEquals(List.of("q", "p"), order(copy));
        assertEquals(source, copy);
    }

    @Test
    void forEachVisitsInInsertionOrder() {
        OrderedObjectMap<String, Integer> map = new OrderedObjectMap<>();
        map.put("b", 2);
        map.put("a", 1);
        map.remove("b");
        map.put("b", 3);
        List<String> seen = new ArrayList<>();
        map.forEach((k, v) -> seen.add(k + v));
        assertEquals(List.of("a1", "b3"), seen);
    }

    @Test
    void rejectsNegativeInitialSize() {
        assertThrows(IllegalArgumentException.class, () -> new OrderedObjectMap<String, String>(-1));
    }
}
