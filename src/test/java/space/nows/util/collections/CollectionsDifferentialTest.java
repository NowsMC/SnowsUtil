package space.nows.util.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.Set;
import org.junit.jupiter.api.Test;
import space.nows.util.array.ArrayEdit;
import space.nows.util.array.ArraySearch;
import space.nows.util.array.ArraySort;
import space.nows.util.collections.IntArrayDeque;
import space.nows.util.collections.IntHashSet;
import space.nows.util.collections.IntIntMap;
import space.nows.util.collections.IntList;
import space.nows.util.collections.IntMinHeap;
import space.nows.util.math.Modulo;
import space.nows.util.math.PowersOfTwo;
import space.nows.util.math.Rounding;
import space.nows.util.random.FastRandom;
import space.nows.util.text.Hex;
import space.nows.util.text.Padding;
import space.nows.util.text.Strings;
import space.nows.util.text.StringSplit;
import static org.junit.jupiter.api.Assertions.*;

class CollectionsDifferentialTest {
    private static final int[] EXTREMES = {0, 1, -1, 2, -2, 127, 128, -128, 255, 256, 65535, 65536,
            Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1};

    private static int randomInt(Random r) {
        return r.nextInt(3) == 0 ? EXTREMES[r.nextInt(EXTREMES.length)] : r.nextInt(64) - 32;
    }

    private static List<Integer> toList(int[] a) {
        List<Integer> out = new ArrayList<>();
        for (int v : a) out.add(v);
        return out;
    }

    private static Set<Integer> toSet(int[] a) {
        Set<Integer> out = new HashSet<>();
        for (int v : a) out.add(v);
        return out;
    }


    @Test
    void intArrayDequeMatchesArrayDeque() {
        Random r = new Random(1);
        for (int trial = 0; trial < 300; trial++) {
            IntArrayDeque d = new IntArrayDeque(r.nextInt(4));
            ArrayDeque<Integer> m = new ArrayDeque<>();
            for (int op = 0; op < 400; op++) {
                int k = r.nextInt(6);
                if (k == 0) { int v = randomInt(r); d.addFirst(v); m.addFirst(v); }
                else if (k == 1) { int v = randomInt(r); d.addLast(v); m.addLast(v); }
                else if (k == 2 && !m.isEmpty()) assertEquals(m.removeFirst(), d.removeFirst());
                else if (k == 3 && !m.isEmpty()) assertEquals(m.removeLast(), d.removeLast());
                assertEquals(m.size(), d.size());
                if (!m.isEmpty()) {
                    assertEquals(m.peekFirst(), d.first());
                    assertEquals(m.peekLast(), d.last());
                    int idx = r.nextInt(m.size());
                    assertEquals(new ArrayList<>(m).get(idx), d.get(idx), "get " + idx);
                }
                assertEquals(new ArrayList<>(m), toList(d.toArray()));
            }
        }
    }

    @Test
    void intListMatchesArrayList() {
        Random r = new Random(2);
        for (int trial = 0; trial < 300; trial++) {
            IntList l = new IntList(r.nextInt(3));
            List<Integer> m = new ArrayList<>();
            for (int op = 0; op < 300; op++) {
                int k = r.nextInt(12);
                int v = randomInt(r);
                if (k <= 1) { l.add(v); m.add(v); }
                else if (k == 2) { int i = r.nextInt(m.size() + 1); l.add(i, v); m.add(i, v); }
                else if (k == 3 && !m.isEmpty()) { int i = r.nextInt(m.size()); assertEquals(m.remove(i), Integer.valueOf(l.removeAt(i))); }
                else if (k == 4 && !m.isEmpty()) {
                    int i = r.nextInt(m.size());
                    int got = l.swapRemove(i);
                    int last = m.size() - 1;
                    assertEquals(m.get(i), got);
                    m.set(i, m.get(last));
                    m.remove(last);
                }
                else if (k == 5) assertEquals(m.remove(Integer.valueOf(v)), l.removeValue(v));
                else if (k == 6 && !m.isEmpty()) assertEquals(m.remove(m.size() - 1), Integer.valueOf(l.pop()));
                else if (k == 7) { int[] more = {v, v + 1, v}; l.addAll(more); for (int x : more) m.add(x); }
                else if (k == 8) { l.reverse(); java.util.Collections.reverse(m); }
                else if (k == 9) { l.sort(); m.sort(null); }
                else if (k == 10 && !m.isEmpty()) { int i = r.nextInt(m.size()); l.set(i, v); m.set(i, v); }
                assertEquals(m.size(), l.size());
                assertEquals(m, toList(l.toArray()), "trial " + trial + " op " + op + " k " + k);
                assertEquals(m.indexOf(v), l.indexOf(v));
                assertEquals(m.lastIndexOf(v), l.lastIndexOf(v), "lastIndexOf");
            }
        }
    }

    @Test
    void intMinHeapMatchesPriorityQueue() {
        Random r = new Random(3);
        for (int trial = 0; trial < 300; trial++) {
            IntMinHeap h = new IntMinHeap(r.nextInt(3));
            PriorityQueue<Integer> q = new PriorityQueue<>();
            for (int op = 0; op < 300; op++) {
                if (r.nextBoolean() || q.isEmpty()) { int v = randomInt(r); h.add(v); q.add(v); }
                else assertEquals(q.poll(), Integer.valueOf(h.poll()));
                assertEquals(q.size(), h.size());
                if (!q.isEmpty()) assertEquals(q.peek(), Integer.valueOf(h.peek()));
            }
        }
    }

    @Test
    void intHashSetMatchesHashSet() {
        Random r = new Random(4);
        for (int trial = 0; trial < 200; trial++) {
            IntHashSet s = new IntHashSet(r.nextInt(3));
            Set<Integer> m = new HashSet<>();
            for (int op = 0; op < 800; op++) {
                int v = r.nextInt(3) == 0 ? randomInt(r) : r.nextInt(40) - 20;
                int k = r.nextInt(5);
                if (k <= 1) assertEquals(m.add(v), s.add(v));
                else if (k <= 3) assertEquals(m.remove(v), s.remove(v), "remove " + v);
                else if (k == 4) s.trim();
                assertEquals(m.size(), s.size());
                assertEquals(m.contains(v), s.contains(v), "contains " + v);
            }
            assertEquals(m, toSet(s.toArray()));
        }
    }

    @Test
    void intIntMapMatchesHashMap() {
        Random r = new Random(5);
        for (int trial = 0; trial < 200; trial++) {
            int noEntry = r.nextInt(3) - 1;
            IntIntMap map = new IntIntMap(r.nextInt(3), 0.6f, noEntry);
            Map<Integer, Integer> m = new HashMap<>();
            for (int op = 0; op < 800; op++) {
                int key = r.nextInt(3) == 0 ? randomInt(r) : r.nextInt(40) - 20;
                int val = randomInt(r);
                int k = r.nextInt(6);
                if (k <= 1) { Integer old = m.put(key, val); assertEquals(old == null ? noEntry : old, map.put(key, val), "put"); }
                else if (k == 2) assertEquals(m.getOrDefault(key, noEntry), map.get(key), "get " + key);
                else if (k == 3) { Integer old = m.remove(key); assertEquals(old == null ? noEntry : old, map.remove(key), "remove " + key); }
                else if (k == 4) { m.merge(key, 3, Integer::sum); map.addTo(key, 3); }
                else assertEquals(m.containsKey(key), map.containsKey(key), "containsKey " + key);
                assertEquals(m.size(), map.size(), "size");
            }
            for (Map.Entry<Integer, Integer> e : m.entrySet()) assertEquals(e.getValue(), map.get(e.getKey()));
        }
    }

}
