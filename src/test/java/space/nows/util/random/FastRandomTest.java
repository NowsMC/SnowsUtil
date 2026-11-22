package space.nows.util.random;

import org.junit.jupiter.api.Test;
import space.nows.util.array.ArrayReorder;

import java.util.Arrays;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class FastRandomTest {
    @Test void defaultConstructorsDoNotShareSequences() {
        for (int i = 0; i < 1000; i++) {
            FastRandom a = new FastRandom();
            FastRandom b = new FastRandom();
            assertNotEquals(a.nextLong(), b.nextLong());
        }
    }

    @Test void sameSeedSameSequenceAndCopiesContinueIdentically() {
        FastRandom a = new FastRandom(123), b = new FastRandom(123);
        for (int i = 0; i < 100; i++) assertEquals(a.nextLong(), b.nextLong());
        FastRandom c = a.copy();
        for (int i = 0; i < 100; i++) assertEquals(a.nextLong(), c.nextLong());
        assertNotEquals(new FastRandom(123).nextLong(), new FastRandom(124).nextLong());
        a.setSeed(123);
        assertEquals(new FastRandom(123).nextLong(), a.nextLong());
        assertDoesNotThrow(() -> new FastRandom(0).nextLong());
    }

    @Test void matchesTheReferenceXoshiro256StarStar() {
        FastRandom r = new FastRandom(0);
        long s0 = 0, s1 = 0, s2 = 0, s3 = 0;
        long z = 0;
        long[] state = new long[4];
        for (int i = 0; i < 4; i++) {
            z += 0x9E3779B97F4A7C15L;
            long x = z;
            x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
            x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
            state[i] = x ^ (x >>> 31);
        }
        s0 = state[0]; s1 = state[1]; s2 = state[2]; s3 = state[3];
        for (int i = 0; i < 1000; i++) {
            long expected = Long.rotateLeft(s1 * 5, 7) * 9;
            long t = s1 << 17;
            s2 ^= s0; s3 ^= s1; s1 ^= s2; s0 ^= s3; s2 ^= t; s3 = Long.rotateLeft(s3, 45);
            assertEquals(expected, r.nextLong());
        }
    }

    @Test void isAJdkRandomGenerator() {
        RandomGenerator g = new FastRandom(7);
        assertEquals(10, g.ints(5_000, 0, 10).distinct().count());
        assertTrue(g.longs(1_000, 0L, 1_000_000_000_000L).allMatch(v -> v >= 0 && v < 1_000_000_000_000L));
        assertTrue(g.doubles(1_000).allMatch(v -> v >= 0.0 && v < 1.0));
    }

    @Test void boundedIntsAreUniform() {
        FastRandom r = new FastRandom(8);
        int[] buckets = new int[10];
        for (int i = 0; i < 100_000; i++) buckets[r.nextInt(10)]++;
        for (int b : buckets) assertTrue(b > 9_000 && b < 11_000, Arrays.toString(buckets));
        assertThrows(IllegalArgumentException.class, () -> r.nextInt(0));
        assertThrows(IllegalArgumentException.class, () -> r.nextInt(5, 5));
        for (int i = 0; i < 20_000; i++) {
            int v = r.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
            assertTrue(v < Integer.MAX_VALUE);
            int w = r.nextInt(-1_500_000_000, 1_500_000_000);
            assertTrue(w >= -1_500_000_000 && w < 1_500_000_000);
        }
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
        assertThrows(IllegalArgumentException.class, () -> r.nextFloat(Float.NaN, 1f));
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

    @Test void lowBitsAreNotPatterned() {
        FastRandom r = new FastRandom(13);
        int ones = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) ones += (int) (r.nextLong() & 1L);
        assertTrue(Math.abs(ones - n / 2) < 1_500, "ones=" + ones);
    }

    @Test void shufflesArePermutationsAndUniform() {
        FastRandom r = new FastRandom(12);
        int[] ints = new int[30];
        long[] longs = new long[30];
        byte[] bytes = new byte[30];
        Integer[] boxed = new Integer[30];
        for (int i = 0; i < 30; i++) { ints[i] = i; longs[i] = i; bytes[i] = (byte) i; boxed[i] = i; }
        int[] i2 = ints.clone();
        ArrayReorder.shuffle(i2, r);
        ArrayReorder.shuffle(longs, r);
        ArrayReorder.shuffle(bytes, r);
        ArrayReorder.shuffle(boxed, r);
        assertFalse(Arrays.equals(ints, i2));
        Arrays.sort(i2);
        assertArrayEquals(ints, i2);
        Arrays.sort(longs);
        Arrays.sort(bytes);
        Arrays.sort(boxed);
        for (int i = 0; i < 30; i++) {
            assertEquals(i, longs[i]);
            assertEquals(i, bytes[i]);
            assertEquals(i, (int) boxed[i]);
        }
        ArrayReorder.shuffle(new int[0], r);
        ArrayReorder.shuffle(new int[1], r);
        int[][] counts = new int[3][3];
        for (int round = 0; round < 60_000; round++) {
            int[] a = {0, 1, 2};
            ArrayReorder.shuffle(a, r);
            for (int pos = 0; pos < 3; pos++) counts[pos][a[pos]]++;
        }
        for (int[] row : counts) for (int c : row) assertTrue(c > 19_000 && c < 21_000, Arrays.deepToString(counts));
        ArrayReorder.shuffle(new int[] {3, 2, 1});
        assertThrows(NullPointerException.class, () -> ArrayReorder.shuffle(new int[3], null));
    }
}
