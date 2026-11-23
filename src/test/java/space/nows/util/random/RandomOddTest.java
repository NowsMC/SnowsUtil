package space.nows.util.random;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RandomOddTest {
    @Test
    void sameSeedGivesSameSequenceAndCopyContinuesIt() {
        FastRandom a = new FastRandom(42L);
        FastRandom b = new FastRandom(42L);
        for (int i = 0; i < 1000; i++) assertEquals(a.nextLong(), b.nextLong());
        FastRandom copy = a.copy();
        for (int i = 0; i < 1000; i++) assertEquals(a.nextLong(), copy.nextLong());
    }

    @Test
    void setSeedRestartsTheSequence() {
        FastRandom r = new FastRandom(7L);
        long first = r.nextLong();
        r.nextLong();
        r.setSeed(7L);
        assertEquals(first, r.nextLong());
    }

    @Test
    void doublesAreInHalfOpenUnitInterval() {
        FastRandom r = new FastRandom(9L);
        for (int i = 0; i < 100_000; i++) {
            double d = r.nextDouble();
            assertTrue(d >= 0.0 && d < 1.0, "nextDouble " + d);
            float f = r.nextFloat();
            assertTrue(f >= 0f && f < 1f, "nextFloat " + f);
        }
    }

    @Test
    void boundedIntsAreRoughlyUniform() {
        FastRandom r = new FastRandom(11L);
        int buckets = 7;
        int[] counts = new int[buckets];
        int draws = 140_000;
        for (int i = 0; i < draws; i++) counts[r.nextInt(buckets)]++;
        double expected = draws / (double) buckets;
        for (int c : counts) assertTrue(Math.abs(c - expected) < expected * 0.05, "bucket " + c);
    }

    @Test
    void rangeAndBoundDegenerateCases() {
        FastRandom r = new FastRandom(13L);
        for (int i = 0; i < 1000; i++) assertEquals(0, r.nextInt(1));
        assertEquals(Integer.MIN_VALUE, r.nextInt(Integer.MIN_VALUE, Integer.MIN_VALUE + 1));
        assertThrows(IllegalArgumentException.class, () -> r.nextInt(3, 2));
    }

    @Test
    void trueRandomEdgeLengthsAndSingleton() {
        TrueRandom t = TrueRandom.shared();
        assertSame(t, TrueRandom.shared());
        assertEquals(0, t.bytes(0).length);
        assertEquals("", t.hex(0));
        assertThrows(IllegalArgumentException.class, () -> t.bytes(-1));
        assertEquals("", t.string(0, "abc"));
        assertThrows(IllegalArgumentException.class, () -> t.string(5, ""), "empty alphabet has nothing to draw from");
        assertThrows(NullPointerException.class, () -> t.string(5, null));
    }

    @Test
    void uuidIsVersionFourRfcVariant() {
        TrueRandom t = TrueRandom.create();
        for (int i = 0; i < 1000; i++) {
            UUID u = t.uuid();
            assertEquals(4, u.version());
            assertEquals(2, u.variant());
        }
    }

    @Test
    void stringsUseOnlyTheAlphabet() {
        TrueRandom t = TrueRandom.create();
        String alphabet = "xyz";
        for (int i = 0; i < 200; i++) {
            for (char c : t.string(20, alphabet).toCharArray()) assertTrue(alphabet.indexOf(c) >= 0, "char " + c);
        }
    }
}
