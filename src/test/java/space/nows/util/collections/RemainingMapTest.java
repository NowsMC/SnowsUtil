package space.nows.util.collections;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RemainingMapTest {
    @Test
    void longLongMapMatchesHashMapIncludingExtremeKeys() {
        Random r = new Random(141);
        long[] special = {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 1L << 40};
        for (int trial = 0; trial < 100; trial++) {
            LongLongMap map = new LongLongMap(r.nextInt(3), 0.6f, -7L);
            Map<Long, Long> ref = new HashMap<>();
            for (int op = 0; op < 800; op++) {
                long key = r.nextBoolean() ? special[r.nextInt(special.length)] : r.nextInt(60) - 30;
                long value = r.nextLong();
                switch (r.nextInt(5)) {
                    case 0, 1 -> {
                        Long old = ref.put(key, value);
                        assertEquals(old == null ? -7L : old, map.put(key, value));
                    }
                    case 2 -> {
                        Long old = ref.remove(key);
                        assertEquals(old == null ? -7L : old, map.remove(key));
                    }
                    case 3 -> assertEquals(ref.getOrDefault(key, -7L), map.get(key));
                    default -> assertEquals(ref.containsKey(key), map.containsKey(key));
                }
                assertEquals(ref.size(), map.size());
            }
        }
    }

    @Test
    void longObjectMapMatchesHashMapWithNullValues() {
        Random r = new Random(142);
        long[] special = {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE};
        LongObjectMap<String> map = new LongObjectMap<>();
        Map<Long, String> ref = new HashMap<>();
        for (int op = 0; op < 5000; op++) {
            long key = r.nextBoolean() ? special[r.nextInt(special.length)] : r.nextInt(100) - 50;
            String value = r.nextInt(4) == 0 ? null : "v" + op;
            switch (r.nextInt(3)) {
                case 0 -> assertEquals(ref.put(key, value), map.put(key, value));
                case 1 -> assertEquals(ref.remove(key), map.remove(key));
                default -> assertEquals(ref.get(key), map.get(key));
            }
            assertEquals(ref.containsKey(key), map.containsKey(key));
            assertEquals(ref.size(), map.size());
        }
    }

    @Test
    void objectLongMapRejectsNullKeysAndCountsLikeHashMap() {
        assertThrows(NullPointerException.class, () -> new ObjectLongMap<String>().addTo(null, 1));
        Random r = new Random(143);
        ObjectLongMap<String> map = new ObjectLongMap<>();
        Map<String, Long> ref = new HashMap<>();
        for (int op = 0; op < 5000; op++) {
            String key = "w" + r.nextInt(40);
            long delta = r.nextInt(7) - 3;
            map.addTo(key, delta);
            ref.merge(key, delta, Long::sum);
            if (r.nextInt(5) == 0) {
                assertEquals(ref.getOrDefault(key, 0L), map.get(key));
                assertEquals(ref.containsKey(key), map.containsKey(key));
            }
            assertEquals(ref.size(), map.size());
        }
    }

    @Test
    void growthRoundsUpToPowersOfTwoAndCapsAtMaximum() {
        assertEquals(8, Growth.powerOfTwo(7));
        assertEquals(16, Growth.powerOfTwo(16));
        assertEquals(32, Growth.powerOfTwo(17));
        assertTrue(Growth.arrayLength(10, 5) >= 10, "never shrinks below the current size");
        assertTrue(Growth.arrayLength(Growth.MAX_LENGTH, Growth.MAX_LENGTH) <= Growth.MAX_LENGTH);
    }
}
