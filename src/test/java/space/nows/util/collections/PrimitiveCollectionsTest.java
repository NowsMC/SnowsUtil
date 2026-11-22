package space.nows.util.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PrimitiveCollectionsTest {
    @Test void intListMatchesArrayList() {
        Random r = new Random(1);
        IntList list = new IntList();
        List<Integer> ref = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            int op = r.nextInt(7);
            int v = r.nextInt(50);
            switch (op) {
                case 0, 1, 2 -> { list.add(v); ref.add(v); }
                case 3 -> { int at = r.nextInt(ref.size() + 1); list.add(at, v); ref.add(at, v); }
                case 4 -> { if (!ref.isEmpty()) { int at = r.nextInt(ref.size()); assertEquals(ref.remove(at), list.removeAt(at)); } }
                case 5 -> assertEquals(ref.remove((Integer) v), list.removeValue(v));
                default -> assertEquals(ref.indexOf(v), list.indexOf(v));
            }
            assertEquals(ref.size(), list.size());
        }
        assertEquals(ref.hashCode(), list.hashCode());
        assertEquals(ref.toString(), list.toString());
        int[] arr = list.toArray();
        for (int i = 0; i < arr.length; i++) assertEquals(ref.get(i), arr[i]);
        assertEquals(list, list.copy());
    }

    @Test void intListGuardsAndHelpers() {
        IntList list = IntList.of(3, 1, 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 0));
        assertEquals(3, list.first());
        assertEquals(2, list.last());
        list.sort();
        assertEquals("[1, 2, 3]", list.toString());
        list.reverse();
        assertEquals("[3, 2, 1]", list.toString());
        assertEquals(3, list.swapRemove(0));
        assertEquals("[1, 2]", list.toString());
        assertEquals(2, list.pop());
        list.trimToSize();
        assertEquals(1, list.capacity());
        list.clear();
        assertTrue(list.isEmpty());
        assertThrows(NoSuchElementException.class, list::pop);
        assertThrows(NoSuchElementException.class, list::first);
        IntList it = IntList.of(1, 2);
        PrimitiveIterator.OfInt iter = it.iterator();
        assertEquals(1, iter.nextInt());
        it.add(9);
        assertThrows(ConcurrentModificationException.class, iter::nextInt);
        assertThrows(ConcurrentModificationException.class, () -> it.forEach(v -> it.add(v)));
        assertEquals(3, IntList.of(5, 6, 7).stream().count());
    }

    @Test void longListBasics() {
        LongList list = new LongList(0);
        for (long i = 0; i < 100; i++) list.add(i << 33);
        assertEquals(100, list.size());
        assertEquals(50L << 33, list.get(50));
        assertEquals(50L << 33, list.removeAt(50));
        assertEquals(-1, list.indexOf(50L << 33));
        assertEquals(99, list.size());
        list.addAll(LongList.of(1, 2, 3));
        assertEquals(3L, list.last());
        assertEquals(102, list.size());
        assertEquals(List.of(1L, 2L, 3L).hashCode(), LongList.of(1, 2, 3).hashCode());
        assertArrayEquals(new long[] {1, 2}, LongList.of(1, 2, 3).toArray(0, 2));
    }

    @Test void longHashSetMatchesHashSet() {
        Random r = new Random(2);
        LongHashSet set = new LongHashSet(2);
        Set<Long> ref = new HashSet<>();
        for (int i = 0; i < 20000; i++) {
            long v = r.nextInt(300) == 0 ? 0L : (long) r.nextInt(2000) << (r.nextBoolean() ? 32 : 0);
            switch (r.nextInt(3)) {
                case 0, 1 -> assertEquals(ref.add(v), set.add(v));
                default -> assertEquals(ref.remove(v), set.remove(v));
            }
            assertEquals(ref.contains(v), set.contains(v));
            assertEquals(ref.size(), set.size());
        }
        assertEquals(ref, set.toJavaSet());
        assertEquals(ref.hashCode(), set.hashCode());
        assertEquals(set, set.copy());
        Set<Long> seen = new HashSet<>();
        PrimitiveIterator.OfLong it = set.iterator();
        while (it.hasNext()) assertTrue(seen.add(it.nextLong()));
        assertEquals(ref, seen);
        set.trim();
        assertEquals(ref, set.toJavaSet());
        set.clear();
        assertTrue(set.isEmpty());
        assertFalse(set.contains(0L));
    }

    @Test void longLongMapMatchesHashMap() {
        Random r = new Random(3);
        LongLongMap map = new LongLongMap(2, 0.6f, -1L);
        Map<Long, Long> ref = new HashMap<>();
        for (int i = 0; i < 20000; i++) {
            long k = r.nextInt(300) == 0 ? 0L : (long) r.nextInt(1500) << 31;
            long v = r.nextLong();
            switch (r.nextInt(6)) {
                case 0 -> { Long old = ref.put(k, v); assertEquals(old == null ? -1L : old, map.put(k, v)); }
                case 1 -> assertEquals(ref.merge(k, 3L, Long::sum), map.addTo(k, 3));
                case 2 -> { Long old = ref.remove(k); assertEquals(old == null ? -1L : old, map.remove(k)); }
                case 3 -> assertEquals(ref.merge(k, v, Long::max), map.merge(k, v, Long::max));
                case 4 -> { Long old = ref.putIfAbsent(k, v); assertEquals(old == null ? -1L : old, map.putIfAbsent(k, v)); }
                default -> assertEquals(ref.getOrDefault(k, -1L), map.get(k));
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

    @Test void intLongMapMatchesHashMap() {
        Random r = new Random(4);
        IntLongMap map = new IntLongMap();
        Map<Integer, Long> ref = new HashMap<>();
        for (int i = 0; i < 20000; i++) {
            int k = r.nextInt(300) == 0 ? 0 : r.nextInt(1500);
            long v = r.nextLong();
            switch (r.nextInt(5)) {
                case 0 -> { Long old = ref.put(k, v); assertEquals(old == null ? 0L : old, map.put(k, v)); }
                case 1 -> assertEquals(ref.merge(k, 5L, Long::sum), map.addTo(k, 5));
                case 2 -> { Long old = ref.remove(k); assertEquals(old == null ? 0L : old, map.remove(k)); }
                case 3 -> assertEquals(ref.merge(k, v, Long::min), map.merge(k, v, Long::min));
                default -> { Long old = ref.putIfAbsent(k, v); assertEquals(old == null ? 0L : old, map.putIfAbsent(k, v)); }
            }
            assertEquals(ref.size(), map.size());
        }
        assertEquals(ref, map.toJavaMap());
        assertEquals(ref.hashCode(), map.hashCode());
        assertEquals(map, map.copy());
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test void trimShrinksAndKeepsContents() {
        IntIntMap map = new IntIntMap(2);
        for (int i = 1; i <= 10000; i++) map.put(i, -i);
        for (int i = 51; i <= 10000; i++) map.remove(i);
        map.trim();
        assertEquals(50, map.size());
        for (int i = 1; i <= 50; i++) assertEquals(-i, map.get(i));
        for (int i = 51; i <= 200; i++) map.put(i, i);
        assertEquals(200, map.size());
        IntObjectMap<String> objects = new IntObjectMap<>();
        for (int i = 1; i < 1000; i++) objects.put(i, "v" + i);
        objects.clear();
        objects.trim();
        assertTrue(objects.isEmpty());
        objects.put(7, "x");
        assertEquals("x", objects.get(7));
    }

    @Test void intArrayDequeMatchesArrayDeque() {
        Random r = new Random(5);
        IntArrayDeque dq = new IntArrayDeque(1);
        ArrayDeque<Integer> ref = new ArrayDeque<>();
        for (int i = 0; i < 20000; i++) {
            int v = r.nextInt();
            switch (r.nextInt(5)) {
                case 0 -> { dq.addFirst(v); ref.addFirst(v); }
                case 1, 2 -> { dq.addLast(v); ref.addLast(v); }
                case 3 -> { if (!ref.isEmpty()) assertEquals(ref.removeFirst(), dq.removeFirst()); }
                default -> { if (!ref.isEmpty()) assertEquals(ref.removeLast(), dq.removeLast()); }
            }
            assertEquals(ref.size(), dq.size());
            if (!ref.isEmpty()) {
                assertEquals(ref.peekFirst(), dq.first());
                assertEquals(ref.peekLast(), dq.last());
            }
        }
        int[] out = dq.toArray();
        int i = 0;
        for (int v : ref) assertEquals(v, out[i++]);
        assertEquals(ref.size(), i);
        assertThrows(NoSuchElementException.class, () -> new IntArrayDeque().removeFirst());
        assertThrows(NoSuchElementException.class, () -> new IntArrayDeque().last());
    }

    @Test void intMinHeapMatchesPriorityQueue() {
        Random r = new Random(6);
        IntMinHeap heap = new IntMinHeap(1);
        PriorityQueue<Integer> ref = new PriorityQueue<>();
        for (int i = 0; i < 20000; i++) {
            if (r.nextInt(3) != 0 || ref.isEmpty()) {
                int v = r.nextInt(1000) - 500;
                heap.add(v);
                ref.add(v);
            } else {
                assertEquals(ref.peek(), heap.peek());
                assertEquals(ref.poll(), heap.poll());
            }
            assertEquals(ref.size(), heap.size());
        }
        int[] seed = new Random(7).ints(500, -1000, 1000).toArray();
        IntMinHeap built = IntMinHeap.of(seed);
        int[] sorted = seed.clone();
        java.util.Arrays.sort(sorted);
        for (int v : sorted) assertEquals(v, built.poll());
        assertTrue(built.isEmpty());
        assertThrows(NoSuchElementException.class, built::poll);
        assertThrows(NoSuchElementException.class, built::peek);
    }
}
