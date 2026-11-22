package space.nows.util.random;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrueRandomTest {
    @Test void independentOutputs() {
        TrueRandom r = TrueRandom.shared();
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) assertTrue(seen.add(r.nextLong()));
        assertNotEquals(TrueRandom.create().nextLong(), TrueRandom.create().nextLong());
        assertSame(TrueRandom.shared(), TrueRandom.shared());
    }

    @Test void boundedValuesAreUniform() {
        TrueRandom r = TrueRandom.create();
        int[] buckets = new int[6];
        for (int i = 0; i < 120_000; i++) buckets[r.nextInt(6)]++;
        for (int b : buckets) assertTrue(b > 19_000 && b < 21_000, Arrays.toString(buckets));
        for (int i = 0; i < 1_000; i++) {
            long v = r.nextLong(1L << 40, (1L << 40) + 17);
            assertTrue(v >= 1L << 40 && v < (1L << 40) + 17);
        }
    }

    @Test void bytesAndHex() {
        TrueRandom r = TrueRandom.shared();
        assertEquals(0, r.bytes(0).length);
        assertEquals(33, r.bytes(33).length);
        assertFalse(Arrays.equals(r.bytes(32), new byte[32]));
        assertEquals(64, r.hex(32).length());
        assertTrue(r.hex(16).matches("[0-9a-f]{32}"));
        assertNotEquals(r.hex(16), r.hex(16));
        assertThrows(IllegalArgumentException.class, () -> r.bytes(-1));
    }

    @Test void uuidIsVersion4Variant2() {
        TrueRandom r = TrueRandom.shared();
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            UUID u = r.uuid();
            assertEquals(4, u.version());
            assertEquals(2, u.variant());
            assertTrue(seen.add(u));
        }
    }

    @Test void stringsUseOnlyTheAlphabet() {
        TrueRandom r = TrueRandom.shared();
        String s = r.string(500, "abc");
        assertEquals(500, s.length());
        assertTrue(s.chars().allMatch(c -> c == 'a' || c == 'b' || c == 'c'));
        assertTrue(s.contains("a") && s.contains("b") && s.contains("c"));
        assertEquals("", r.string(0, "x"));
        assertEquals("zzzz", r.string(4, "z"));
        assertThrows(IllegalArgumentException.class, () -> r.string(4, ""));
        assertThrows(IllegalArgumentException.class, () -> r.string(-1, "a"));
    }

    @Test void wrapsAnyGivenSecureRandomAndStrongInstance() {
        assertNotNull(TrueRandom.of(new SecureRandom()).nextLong());
        assertThrows(NullPointerException.class, () -> TrueRandom.of(null));
        assertDoesNotThrow(() -> TrueRandom.strong().nextInt());
    }
}
