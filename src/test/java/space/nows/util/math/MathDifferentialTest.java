package space.nows.util.math;

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

class MathDifferentialTest {
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

}
