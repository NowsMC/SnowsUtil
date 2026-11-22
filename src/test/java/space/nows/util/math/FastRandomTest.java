package space.nows.util.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FastRandomTest {
    @Test void defaultConstructorsDoNotShareSequences() {
        for (int i = 0; i < 1000; i++) {
            FastRandom a = new FastRandom();
            FastRandom b = new FastRandom();
            assertNotEquals(a.nextLong(), b.nextLong());
        }
    }

    @Test void sameSeedSameSequence() {
        FastRandom a = new FastRandom(42);
        FastRandom b = new FastRandom(42);
        for (int i = 0; i < 100; i++) assertEquals(a.nextLong(), b.nextLong());
    }
}
