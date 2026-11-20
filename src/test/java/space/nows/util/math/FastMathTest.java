package space.nows.util.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FastMathTest {
    @Test void scalarHelpers() {
        assertEquals(5, FastMath.clamp(9, 0, 5));
        assertEquals(-2, FastMath.fastFloor(-1.5f));
        assertEquals(-1, FastMath.fastCeil(-1.5f));
        assertEquals(3, FastMath.fastAbs(-3));
        assertEquals(2.5f, FastMath.fastAbs(-2.5f));
    }

    @Test void interpolation() {
        assertEquals(15f, Interpolation.lerp(10f, 20f, 0.5f), 1e-6);
        assertEquals(0.5f, Interpolation.inverseLerp(10f, 20f, 15f), 1e-6);
        assertEquals(0.5f, Interpolation.smoothstep(0f, 1f, 0.5f), 1e-6);
    }

    @Test void intMath() {
        assertEquals(16, IntMath.nextPowerOfTwo(9));
        assertEquals(3, IntMath.log2(8));
    }

    @Test void trigTable() {
        assertEquals(Math.sin(1.0), FastTrig.sin(1f), 0.005);
        assertEquals(Math.cos(-2.0), FastTrig.cos(-2f), 0.005);
    }

    @Test void random() {
        FastRandom a = new FastRandom(123), b = new FastRandom(123);
        for (int i = 0; i < 100; i++) assertEquals(a.nextLong(), b.nextLong());
        for (int i = 0; i < 10_000; i++) {
            int v = a.nextInt(10);
            assertTrue(v >= 0 && v < 10);
            double d = a.nextDouble();
            assertTrue(d >= 0 && d < 1);
        }
    }
}
