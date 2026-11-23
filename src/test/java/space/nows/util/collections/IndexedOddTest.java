package space.nows.util.collections;

import org.junit.jupiter.api.Test;
import space.nows.util.collections.indexed.EnumArrayMap;
import space.nows.util.collections.indexed.IntRangeArrayMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IndexedOddTest {
    enum Color { RED, GREEN, BLUE }

    @Test
    void enumArrayMapMatchesHashMap() {
        Random r = new Random(111);
        EnumArrayMap<Color, Integer> map = new EnumArrayMap<>(Color.class);
        Map<Color, Integer> ref = new HashMap<>();
        Color[] colors = Color.values();
        for (int op = 0; op < 5000; op++) {
            Color c = colors[r.nextInt(colors.length)];
            switch (r.nextInt(4)) {
                case 0, 1 -> assertEquals(ref.put(c, op), map.put(c, op));
                case 2 -> assertEquals(ref.remove(c), map.remove(c));
                default -> assertEquals(ref.get(c), map.get(c));
            }
            assertEquals(ref.size(), map.size());
            assertEquals(ref.containsKey(c), map.containsKey(c));
        }
        assertEquals(ref, map.toEnumMap());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.copy().size());
    }

    @Test
    void intRangeArrayMapHandlesBoundsAndRejectsOutside() {
        IntRangeArrayMap<String> map = new IntRangeArrayMap<>(-3, 3);
        assertEquals(-3, map.minKey());
        assertEquals(3, map.maxKey());
        map.put(-3, "lo");
        map.put(3, "hi");
        assertEquals("lo", map.get(-3));
        assertEquals("hi", map.get(3));
        assertFalse(map.containsKey(4));
        assertThrows(IndexOutOfBoundsException.class, () -> map.put(4, "out"));
        assertNull(map.get(-4), "get outside the range returns null, as Map.get does");
        assertEquals(2, map.size());
        assertThrows(IllegalArgumentException.class, () -> new IntRangeArrayMap<String>(5, 4));
    }

    @Test
    void intRangeArrayMapAtIntegerExtremes() {
        IntRangeArrayMap<String> map = new IntRangeArrayMap<>(Integer.MAX_VALUE - 2, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, map.maxKey(), "maxKey must not overflow at Integer.MAX_VALUE");
        map.put(Integer.MAX_VALUE, "top");
        assertEquals("top", map.get(Integer.MAX_VALUE));
    }

    @Test
    void longListMatchesArrayList() {
        Random r = new Random(112);
        LongList list = new LongList(0);
        List<Long> ref = new ArrayList<>();
        for (int op = 0; op < 3000; op++) {
            long v = r.nextLong();
            switch (r.nextInt(4)) {
                case 0, 1 -> { list.add(v); ref.add(v); }
                case 2 -> { if (!ref.isEmpty()) assertEquals(ref.remove(ref.size() - 1), Long.valueOf(list.pop())); }
                default -> {
                    if (!ref.isEmpty()) {
                        int i = r.nextInt(ref.size());
                        assertEquals(ref.get(i), Long.valueOf(list.get(i)));
                    }
                }
            }
            assertEquals(ref.size(), list.size());
        }
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(list.size()));
    }

    @Test
    void longHashSetMatchesHashSet() {
        Random r = new Random(113);
        LongHashSet set = new LongHashSet();
        Set<Long> ref = new HashSet<>();
        long[] keys = {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 1L << 32};
        for (int op = 0; op < 5000; op++) {
            long key = r.nextBoolean() ? keys[r.nextInt(keys.length)] : r.nextInt(200) - 100;
            if (r.nextBoolean()) assertEquals(ref.add(key), set.add(key));
            else assertEquals(ref.remove(key), set.remove(key));
            assertEquals(ref.size(), set.size());
            assertEquals(ref.contains(key), set.contains(key));
        }
    }

}
