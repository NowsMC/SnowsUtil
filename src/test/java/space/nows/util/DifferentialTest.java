package space.nows.util;

import org.junit.jupiter.api.Test;
import space.nows.util.array.ArrayEdit;
import space.nows.util.array.ArraySearch;
import space.nows.util.array.ArraySort;
import space.nows.util.collections.IntArrayDeque;
import space.nows.util.collections.IntHashSet;
import space.nows.util.collections.IntIntMap;
import space.nows.util.collections.IntList;
import space.nows.util.collections.IntMinHeap;
import space.nows.util.math.PowersOfTwo;
import space.nows.util.math.Modulo;
import space.nows.util.math.Rounding;
import space.nows.util.random.FastRandom;
import space.nows.util.text.Hex;
import space.nows.util.text.Padding;
import space.nows.util.text.StringSplit;
import space.nows.util.text.Strings;

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
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class DifferentialTest {
    private static final int[] EXTREMES = {0, 1, -1, 2, -2, 127, 128, -128, 255, 256, 65535, 65536,
            Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1};

    private static int randomInt(Random r) {
        return r.nextInt(3) == 0 ? EXTREMES[r.nextInt(EXTREMES.length)] : r.nextInt(64) - 32;
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

    private static List<Integer> toList(int[] a) {
        List<Integer> out = new ArrayList<>();
        for (int v : a) out.add(v);
        return out;
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

    private static Set<Integer> toSet(int[] a) {
        Set<Integer> out = new HashSet<>();
        for (int v : a) out.add(v);
        return out;
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

    @Test
    void radixSortMatchesArraysSort() {
        Random r = new Random(6);
        for (int trial = 0; trial < 60; trial++) {
            int n = r.nextInt(5000);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = r.nextInt(4) == 0 ? EXTREMES[r.nextInt(EXTREMES.length)] : r.nextInt();
            int[] expected = a.clone();
            Arrays.sort(expected);
            ArraySort.radixSort(a);
            assertArrayEquals(expected, a, "n=" + n);
        }
        int[] range = {9, 8, 7, 6, 5};
        int[] big = new int[3000];
        for (int i = 0; i < big.length; i++) big[i] = -i;
        int[] expected = big.clone();
        Arrays.sort(expected, 100, 2500);
        ArraySort.radixSort(big, 100, 2500);
        assertArrayEquals(expected, big, "sub-range");
        assertArrayEquals(new int[]{9, 8, 7, 6, 5}, range);
    }

    @Test
    void radixSortLongMatchesArraysSort() {
        Random r = new Random(7);
        for (int trial = 0; trial < 40; trial++) {
            long[] a = new long[r.nextInt(4000)];
            for (int i = 0; i < a.length; i++) a[i] = r.nextBoolean() ? r.nextLong() : (r.nextBoolean() ? Long.MIN_VALUE : Long.MAX_VALUE) - r.nextInt(3);
            long[] expected = a.clone();
            Arrays.sort(expected);
            ArraySort.radixSort(a);
            assertArrayEquals(expected, a);
        }
    }

    @Test
    void byteSearchMatchesNaive() {
        Random r = new Random(8);
        for (int trial = 0; trial < 2000; trial++) {
            byte[] a = new byte[r.nextInt(40)];
            r.nextBytes(a);
            byte v = a.length > 0 && r.nextBoolean() ? a[r.nextInt(a.length)] : (byte) r.nextInt();
            int from = r.nextInt(a.length + 3) - 2;
            int expected = -1;
            for (int i = Math.max(from, 0); i < a.length; i++) if (a[i] == v) { expected = i; break; }
            assertEquals(expected, ArraySearch.indexOf(a, v, from), "from=" + from + " v=" + v + " a=" + Arrays.toString(a));
        }
    }

    @Test
    void arrayEditMatchesLists() {
        Random r = new Random(9);
        for (int trial = 0; trial < 500; trial++) {
            int[] a = new int[r.nextInt(10)];
            for (int i = 0; i < a.length; i++) a[i] = r.nextInt(100);
            int i = r.nextInt(a.length + 1);
            int v = r.nextInt(100);
            List<Integer> m = toList(a);
            m.add(i, v);
            assertEquals(m, toList(ArrayEdit.insert(a, i, v)), "insert");
            if (a.length > 0) {
                int j = r.nextInt(a.length);
                List<Integer> removed = new ArrayList<>(toList(a));
                removed.remove(j);
                assertEquals(removed, toList(ArrayEdit.remove(a, j)), "remove");
            }
        }
    }

    @Test
    void stringSplitMatchesStringSplit() {
        String[] samples = {"", ",", ",,", "a", "a,", ",a", "a,,b", "a,b,,", "x"};
        for (String s : samples) {
            List<String> expected = Arrays.asList(s.split(Pattern.quote(","), -1));
            assertEquals(expected, StringSplit.split(s, ','), "split '" + s + "'");
            assertEquals(s.chars().filter(c -> c == ',').count(), StringSplit.count(s, ','));
        }
    }

    @Test
    void stringsAndPaddingEdgeCases() {
        assertEquals("", Strings.truncate("", 0));
        assertEquals("", Strings.truncate("ab", 0));
        String emoji = "a😀b";
        for (int max = 0; max <= emoji.length() + 1; max++) {
            String t = Strings.truncate(emoji, max);
            assertFalse(t.length() > 0 && Character.isHighSurrogate(t.charAt(t.length() - 1)), "dangling high surrogate at " + max);
            assertTrue(emoji.startsWith(t));
        }
        assertEquals("ab", Padding.padLeft("ab", 1, '0'));
        assertEquals("00ab", Padding.padLeft("ab", 4, '0'));
        assertEquals("ab00", Padding.padRight("ab", 4, '0'));
        assertEquals("", Strings.defaultIfBlank("  ", "") );
        assertEquals("ǅ", Strings.capitalize("ǆ"));
        assertEquals("", Strings.capitalize(""));
    }

    @Test
    void hexMatchesHexFormat() {
        Random r = new Random(10);
        HexFormat hf = HexFormat.of();
        for (int trial = 0; trial < 500; trial++) {
            byte[] data = new byte[r.nextInt(20)];
            r.nextBytes(data);
            assertEquals(hf.formatHex(data), Hex.toHex(data));
            assertEquals(hf.formatHex(data).toUpperCase(), Hex.toUpperHex(data));
            assertArrayEquals(data, Hex.fromHex(hf.formatHex(data)));
            assertArrayEquals(data, Hex.fromHex(hf.formatHex(data).toUpperCase()));
            assertTrue(Hex.isHex(hf.formatHex(data)));
        }
        assertFalse(Hex.isHex("abc"));
        assertFalse(Hex.isHex("zz"));
    }

    @Test
    void modFloatStaysInRange() {
        Random r = new Random(11);
        float[] specials = {0f, -0f, -1e-10f, Float.MIN_VALUE, -Float.MIN_VALUE, 1e10f, -1e10f, 0.99999994f, -0.99999994f};
        for (int trial = 0; trial < 100_000; trial++) {
            float a = trial < specials.length ? specials[trial] : (float) (r.nextGaussian() * 1000);
            float m = r.nextBoolean() ? 1f : 0.1f + r.nextFloat() * 50f;
            float got = Modulo.mod(a, m);
            assertTrue(got >= 0f && got < m, "a=" + a + " m=" + m + " got=" + got);
        }
    }

    @Test
    void powersOfTwoAgreeWithReference() {
        for (int v = 1; v < 5000; v++) {
            assertEquals(Integer.bitCount(v) == 1, PowersOfTwo.isPowerOfTwo(v));
            int next = 1;
            while (next < v) next <<= 1;
            assertEquals(next, PowersOfTwo.nextPowerOfTwo(v), "nextPowerOfTwo " + v);
            assertEquals(Integer.numberOfTrailingZeros(next), PowersOfTwo.ceilLog2(v), "ceilLog2 " + v);
        }
        assertEquals(1 << 30, PowersOfTwo.nextPowerOfTwo(1 << 30));
        assertEquals(0, PowersOfTwo.alignUp(0, 8));
        assertEquals(8, PowersOfTwo.alignUp(1, 8));
        assertEquals(16, PowersOfTwo.alignUp(9, 8));
    }

    @Test
    void roundingMatchesReference() {
        assertEquals(Integer.MAX_VALUE, Rounding.round(1e300));
        assertEquals(Integer.MIN_VALUE, Rounding.round(-1e300));
        assertEquals(3, Rounding.round(2.5));
        assertEquals(-2, Rounding.round(-2.5));
        assertEquals(-3, Rounding.floor(-2.5f) - 0);
        assertEquals(-2, Rounding.ceil(-2.5f));
    }

    @Test
    void fastRandomBoundedStaysInRange() {
        FastRandom rnd = new FastRandom(12345);
        for (int bound : new int[]{1, 2, 3, 7, 1000, 1 << 30, Integer.MAX_VALUE}) {
            for (int i = 0; i < 20_000; i++) {
                int v = rnd.nextInt(bound);
                assertTrue(v >= 0 && v < bound, "bound " + bound + " gave " + v);
            }
        }
        assertThrows(IllegalArgumentException.class, () -> rnd.nextInt(0));
        assertThrows(IllegalArgumentException.class, () -> rnd.nextInt(5, 5));
        for (int i = 0; i < 20_000; i++) {
            int v = rnd.nextInt(-7, 7);
            assertTrue(v >= -7 && v < 7, "range gave " + v);
        }
    }
}
