package space.nows.util.random;

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

class RandomDifferentialTest {
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
