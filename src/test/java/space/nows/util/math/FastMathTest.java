package space.nows.util.math;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class FastMathTest {
    // ---------------------------------------------------------------- FastMath
    @Test void scalarHelpers() {
        assertEquals(5, FastMath.clamp(9, 0, 5));
        assertEquals(-2, FastMath.fastFloor(-1.5f));
        assertEquals(-1, FastMath.fastCeil(-1.5f));
        assertEquals(3, FastMath.fastAbs(-3));
        assertEquals(2.5f, FastMath.fastAbs(-2.5f));
    }

    @Test void floorAndCeilMatchMathInTheIntRange() {
        Random r = new Random(51);
        for (int i = 0; i < 200_000; i++) {
            float f = (r.nextFloat() - 0.5f) * 2e6f;
            double d = (r.nextDouble() - 0.5) * 4e9 / 2.0;
            assertEquals((int) Math.floor(f), FastMath.fastFloor(f));
            assertEquals((int) Math.ceil(f), FastMath.fastCeil(f));
            assertEquals((long) Math.floor(d), FastMath.fastFloor(d));
            assertEquals((long) Math.ceil(d), FastMath.fastCeil(d));
        }
        assertEquals(-1, FastMath.fastFloor(-0.5f));
        assertEquals(0, FastMath.fastFloor(-0.0f));
        assertEquals(1, FastMath.fastCeil(0.0001f));
    }

    @Test void floorAndCeilSaturateInsteadOfOverflowing() {
        assertEquals(Integer.MIN_VALUE, FastMath.fastFloor(-3e9f));     // used to wrap around to Integer.MAX_VALUE
        assertEquals(Integer.MIN_VALUE, FastMath.fastFloor(-3e9));
        assertEquals(Integer.MAX_VALUE, FastMath.fastFloor(3e9f));
        assertEquals(Integer.MAX_VALUE, FastMath.fastCeil(3e9f));       // used to wrap around to Integer.MIN_VALUE
        assertEquals(Integer.MAX_VALUE, FastMath.fastCeil(3e9));
        assertEquals(Integer.MIN_VALUE, FastMath.fastCeil(-3e9));
        assertEquals(0, FastMath.fastFloor(Float.NaN));
        assertEquals(Integer.MAX_VALUE, FastMath.fastFloor(Float.POSITIVE_INFINITY));
    }

    @Test void roundIsExactHalfUp() {
        assertEquals(0, FastMath.fastRound(0.49999997f));              // floor(v + 0.5f) misrounded this to 1
        assertEquals(3, FastMath.fastRound(2.5f));
        assertEquals(-2, FastMath.fastRound(-2.5f));
        assertEquals(-3, FastMath.fastRound(-2.6f));
        assertEquals(0, FastMath.fastRound(Float.NaN));
        assertEquals(Integer.MAX_VALUE, FastMath.fastRound(1e20f));
        assertEquals(Integer.MAX_VALUE, FastMath.fastRound(1e20));
        assertEquals(Integer.MIN_VALUE, FastMath.fastRound(-1e20));
        assertEquals(0, FastMath.fastRound(0.49999999999999994));
        assertEquals(4, FastMath.fastRound(3.5));
    }

    @Test void moduloHasTheSignOfTheDivisorAndStaysInRange() {
        assertEquals(4, FastMath.mod(-1, 5));
        assertEquals(0, FastMath.mod(10, 5));
        assertEquals(Math.floorMod(5, -3), FastMath.mod(5, -3));        // negative divisors used to give wrong answers
        assertThrows(ArithmeticException.class, () -> FastMath.mod(1, 0));
        assertEquals(359f, FastMath.mod(-1f, 360f), 1e-4);
        assertEquals(0.25f, FastMath.mod(5.25f, 1f), 1e-6);
        Random r = new Random(52);
        for (int i = 0; i < 100_000; i++) {
            float a = (r.nextFloat() - 0.5f) * 1e4f;
            float m = 0.001f + r.nextFloat() * 400f;
            float v = FastMath.mod(a, m);
            assertTrue(v >= 0f && v < m, a + " mod " + m + " = " + v);
        }
        float tiny = FastMath.mod(-1e-9f, 360f);                        // -1e-9 + 360 rounds to 360f; the result must still be < 360
        assertTrue(tiny >= 0f && tiny < 360f);
        assertTrue(Float.isNaN(FastMath.mod(Float.NaN, 5f)));
        assertThrows(IllegalArgumentException.class, () -> FastMath.mod(1f, 0f));
        assertThrows(IllegalArgumentException.class, () -> FastMath.mod(1f, -2f));
        assertThrows(IllegalArgumentException.class, () -> FastMath.mod(1f, Float.NaN));
    }

    // ---------------------------------------------------------------- Interpolation / IntMath
    @Test void interpolation() {
        assertEquals(15f, Interpolation.lerp(10f, 20f, 0.5f), 1e-6);
        assertEquals(0.5f, Interpolation.inverseLerp(10f, 20f, 15f), 1e-6);
        assertEquals(0.5f, Interpolation.smoothstep(0f, 1f, 0.5f), 1e-6);
        assertEquals(0f, Interpolation.smoothstep(0f, 1f, -3f), 1e-6);
        assertEquals(1f, Interpolation.smoothstep(0f, 1f, 7f), 1e-6);
    }

    @Test void remapAndAngles() {
        assertEquals(150f, Interpolation.remap(5f, 0f, 10f, 100f, 200f), 1e-4);
        assertEquals(100f, Interpolation.remap(5f, 3f, 3f, 100f, 200f), 1e-4);          // degenerate input range -> outMin, no NaN
        assertEquals(0f, Interpolation.lerpAngle(0.1f, FastMath.TWO_PI - 0.1f, 0.5f), 1e-5);   // goes the short way round
        assertEquals(1f, Interpolation.lerpAngle(1f, 1f, 0.7f), 1e-6);
        assertEquals(0.5f, Interpolation.lerpAngle(0f, 1f, 0.5f), 1e-6);
    }

    @Test void intMath() {
        assertEquals(16, IntMath.nextPowerOfTwo(9));
        assertEquals(3, IntMath.log2(8));
        assertEquals(0, IntMath.ceilLog2(1));
        assertEquals(1, IntMath.ceilLog2(2));
        assertEquals(2, IntMath.ceilLog2(3));
        assertEquals(10, IntMath.ceilLog2(1024));
        assertEquals(11, IntMath.ceilLog2(1025));
        assertEquals(30, IntMath.ceilLog2(1 << 30));
        assertEquals(31, IntMath.ceilLog2(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> IntMath.ceilLog2(0));
        assertThrows(IllegalArgumentException.class, () -> IntMath.ceilLog2(-4));
    }

    @Test void alignUp() {
        assertEquals(8, IntMath.alignUp(5, 4));
        assertEquals(8, IntMath.alignUp(8, 4));
        assertEquals(0, IntMath.alignUp(0, 16));
        assertEquals(-4, IntMath.alignUp(-5, 4));
        assertEquals(7, IntMath.alignUp(7, 1));
        assertThrows(IllegalArgumentException.class, () -> IntMath.alignUp(3, 3));
        assertThrows(IllegalArgumentException.class, () -> IntMath.alignUp(3, 0));
        assertThrows(ArithmeticException.class, () -> IntMath.alignUp(Integer.MAX_VALUE, 2));
    }

    // ---------------------------------------------------------------- FastTrig
    @Test void trigTableStaysCloseToMath() {
        double worst = 0;
        for (float x = -100f; x <= 100f; x += 0.01f) {
            worst = Math.max(worst, Math.abs(Math.sin(x) - FastTrig.sin(x)));
            worst = Math.max(worst, Math.abs(Math.cos(x) - FastTrig.cos(x)));
        }
        assertTrue(worst < 2e-3, "max error " + worst);
        assertEquals(0.0, FastTrig.sin(0f), 1e-6);
        assertEquals(1.0, FastTrig.cos(0f), 1e-6);
        assertEquals(Math.sin(1.0), FastTrig.sin(1f), 0.005);
        assertEquals(Math.cos(-2.0), FastTrig.cos(-2f), 0.005);
    }

    @Test void trigRejectsInputsItCannotHandle() {
        assertThrows(IllegalArgumentException.class, () -> FastTrig.sin(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> FastTrig.cos(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> FastTrig.sin(Float.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> FastTrig.sin(1e7f));
        assertThrows(IllegalArgumentException.class, () -> FastTrig.cos(-1e7f));
        assertDoesNotThrow(() -> FastTrig.sin(1e6f));
        assertDoesNotThrow(() -> FastTrig.cos(-1e6f));
    }

    // ---------------------------------------------------------------- FastRandom
    @Test void sameSeedSameSequenceAndCopiesContinueIdentically() {
        FastRandom a = new FastRandom(123), b = new FastRandom(123);
        for (int i = 0; i < 100; i++) assertEquals(a.nextLong(), b.nextLong());
        FastRandom c = a.copy();
        for (int i = 0; i < 100; i++) assertEquals(a.nextLong(), c.nextLong());
        FastRandom other = new FastRandom(124);
        assertNotEquals(new FastRandom(123).nextLong(), other.nextLong());
        a.setSeed(123);
        assertEquals(new FastRandom(123).nextLong(), a.nextLong());
        assertDoesNotThrow(() -> new FastRandom(0).nextLong());
    }

    @Test void isAJdkRandomGenerator() {
        RandomGenerator g = new FastRandom(7);
        assertEquals(10, g.ints(5_000, 0, 10).distinct().count());
        assertTrue(g.longs(1_000, 0L, 1_000_000_000_000L).allMatch(v -> v >= 0 && v < 1_000_000_000_000L));
        assertTrue(g.doubles(1_000).allMatch(v -> v >= 0.0 && v < 1.0));
        long bound = (1L << 40) + 12_345;
        for (int i = 0; i < 1_000; i++) {
            long v = g.nextLong(bound);
            assertTrue(v >= 0 && v < bound);
        }
    }

    @Test void boundedIntsStayInRangeAndAreRoughlyUniform() {
        FastRandom r = new FastRandom(8);
        int[] buckets = new int[10];
        for (int i = 0; i < 100_000; i++) {
            int v = r.nextInt(10);
            assertTrue(v >= 0 && v < 10);
            buckets[v]++;
        }
        for (int b : buckets) assertTrue(b > 9_000 && b < 11_000, Arrays.toString(buckets));
        for (int i = 0; i < 10_000; i++) {
            int v = r.nextInt(-5, 5);
            assertTrue(v >= -5 && v < 5);
        }
        assertThrows(IllegalArgumentException.class, () -> r.nextInt(0));
        assertThrows(IllegalArgumentException.class, () -> r.nextInt(-1));
        assertThrows(IllegalArgumentException.class, () -> r.nextInt(5, 5));
        assertThrows(IllegalArgumentException.class, () -> r.nextInt(6, 5));
    }

    @Test void intRangesWiderThanIntMaxValueDoNotOverflow() {
        FastRandom r = new FastRandom(9);
        for (int i = 0; i < 20_000; i++) {
            int v = r.nextInt(-1_500_000_000, 1_500_000_000);           // range 3e9 > Integer.MAX_VALUE: used to throw "bound must be positive"
            assertTrue(v >= -1_500_000_000 && v < 1_500_000_000);
            int w = r.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
            assertTrue(w < Integer.MAX_VALUE);
        }
        boolean sawNegative = false, sawPositive = false;
        for (int i = 0; i < 100; i++) {
            int v = r.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
            sawNegative |= v < 0;
            sawPositive |= v > 0;
        }
        assertTrue(sawNegative && sawPositive);
    }

    @Test void floatsDoublesAndGaussians() {
        FastRandom r = new FastRandom(10);
        double sum = 0, sumSq = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            float f = r.nextFloat();
            double d = r.nextDouble();
            assertTrue(f >= 0f && f < 1f);
            assertTrue(d >= 0.0 && d < 1.0);
            float g = r.nextFloat(-2f, 3f);
            assertTrue(g >= -2f && g < 3f);
            double x = r.nextGaussian();
            sum += x;
            sumSq += x * x;
        }
        assertEquals(0.0, sum / n, 0.05);
        assertEquals(1.0, sumSq / n, 0.1);
        assertThrows(IllegalArgumentException.class, () -> r.nextFloat(1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> r.nextFloat(2f, 1f));
        assertThrows(IllegalArgumentException.class, () -> r.nextFloat(Float.NaN, 1f));
        assertThrows(IllegalArgumentException.class, () -> r.nextFloat(-Float.MAX_VALUE, Float.MAX_VALUE));   // range overflows to infinity
    }

    @Test void nextBytesFillsEveryLength() {
        FastRandom r = new FastRandom(11);
        for (int len = 0; len < 40; len++) {
            byte[] b = new byte[len];
            r.nextBytes(b);
            if (len >= 16) assertFalse(Arrays.equals(b, new byte[len]));
        }
        assertThrows(NullPointerException.class, () -> r.nextBytes(null));
    }

    @Test void shufflesArePermutations() {
        FastRandom r = new FastRandom(12);
        int[] ints = new int[30];
        long[] longs = new long[30];
        Integer[] boxed = new Integer[30];
        for (int i = 0; i < 30; i++) { ints[i] = i; longs[i] = i; boxed[i] = i; }
        int[] i2 = ints.clone();
        r.shuffle(i2);
        r.shuffle(longs);
        r.shuffle(boxed);
        assertFalse(Arrays.equals(ints, i2));
        Arrays.sort(i2);
        assertArrayEquals(ints, i2);
        long[] l2 = longs.clone();
        Arrays.sort(l2);
        for (int i = 0; i < 30; i++) assertEquals(i, l2[i]);
        Integer[] b2 = boxed.clone();
        Arrays.sort(b2);
        for (int i = 0; i < 30; i++) assertEquals(i, (int) b2[i]);
        r.shuffle(new int[0]);
        r.shuffle(new int[1]);
    }
}
