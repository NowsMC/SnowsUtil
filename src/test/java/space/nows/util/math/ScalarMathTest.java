package space.nows.util.math;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ScalarMathTest {
    @Test void scalarHelpers() {
        assertEquals(5, Bounds.clamp(9, 0, 5));
        assertEquals(-2, Rounding.floor(-1.5f));
        assertEquals(-1, Rounding.ceil(-1.5f));
    }

    @Test void floorAndCeilMatchMathInTheIntRange() {
        Random r = new Random(51);
        for (int i = 0; i < 200_000; i++) {
            float f = (r.nextFloat() - 0.5f) * 2e6f;
            double d = (r.nextDouble() - 0.5) * 4e9 / 2.0;
            assertEquals((int) Math.floor(f), Rounding.floor(f));
            assertEquals((int) Math.ceil(f), Rounding.ceil(f));
            assertEquals((long) Math.floor(d), Rounding.floor(d));
            assertEquals((long) Math.ceil(d), Rounding.ceil(d));
        }
        assertEquals(-1, Rounding.floor(-0.5f));
        assertEquals(0, Rounding.floor(-0.0f));
        assertEquals(1, Rounding.ceil(0.0001f));
    }

    @Test void floorAndCeilSaturateInsteadOfOverflowing() {
        assertEquals(Integer.MIN_VALUE, Rounding.floor(-3e9f));
        assertEquals(Integer.MIN_VALUE, Rounding.floor(-3e9));
        assertEquals(Integer.MAX_VALUE, Rounding.floor(3e9f));
        assertEquals(Integer.MAX_VALUE, Rounding.ceil(3e9f));
        assertEquals(Integer.MAX_VALUE, Rounding.ceil(3e9));
        assertEquals(Integer.MIN_VALUE, Rounding.ceil(-3e9));
        assertThrows(IllegalArgumentException.class, () -> Rounding.floor(Float.NaN));
        assertEquals(Integer.MAX_VALUE, Rounding.floor(Float.POSITIVE_INFINITY));
    }

    @Test void roundIsExactHalfUp() {
        assertEquals(0, Rounding.round(0.49999997f));
        assertEquals(3, Rounding.round(2.5f));
        assertEquals(-2, Rounding.round(-2.5f));
        assertEquals(-3, Rounding.round(-2.6f));
        assertThrows(IllegalArgumentException.class, () -> Rounding.round(Float.NaN));
        assertEquals(Integer.MAX_VALUE, Rounding.round(1e20f));
        assertEquals(Integer.MAX_VALUE, Rounding.round(1e20));
        assertEquals(Integer.MIN_VALUE, Rounding.round(-1e20));
        assertEquals(0, Rounding.round(0.49999999999999994));
        assertEquals(4, Rounding.round(3.5));
    }

    @Test void moduloHasTheSignOfTheDivisorAndStaysInRange() {
        assertEquals(4, Modulo.mod(-1, 5));
        assertEquals(0, Modulo.mod(10, 5));
        assertEquals(Math.floorMod(5, -3), Modulo.mod(5, -3));
        assertThrows(ArithmeticException.class, () -> Modulo.mod(1, 0));
        assertEquals(359f, Modulo.mod(-1f, 360f), 1e-4);
        assertEquals(0.25f, Modulo.mod(5.25f, 1f), 1e-6);
        Random r = new Random(52);
        for (int i = 0; i < 100_000; i++) {
            float a = (r.nextFloat() - 0.5f) * 1e4f;
            float m = 0.001f + r.nextFloat() * 400f;
            float v = Modulo.mod(a, m);
            assertTrue(v >= 0f && v < m, a + " mod " + m + " = " + v);
        }
        float tiny = Modulo.mod(-1e-9f, 360f);
        assertTrue(tiny >= 0f && tiny < 360f);
        assertTrue(Float.isNaN(Modulo.mod(Float.NaN, 5f)));
        assertThrows(IllegalArgumentException.class, () -> Modulo.mod(1f, 0f));
        assertThrows(IllegalArgumentException.class, () -> Modulo.mod(1f, -2f));
        assertThrows(IllegalArgumentException.class, () -> Modulo.mod(1f, Float.NaN));
    }

    @Test void interpolation() {
        assertEquals(15f, Interpolation.lerp(10f, 20f, 0.5f), 1e-6);
        assertEquals(0.5f, Interpolation.inverseLerp(10f, 20f, 15f), 1e-6);
        assertEquals(0.5f, Interpolation.smoothstep(0f, 1f, 0.5f), 1e-6);
        assertEquals(0f, Interpolation.smoothstep(0f, 1f, -3f), 1e-6);
        assertEquals(1f, Interpolation.smoothstep(0f, 1f, 7f), 1e-6);
    }

    @Test void remapAndAngles() {
        assertEquals(150f, Interpolation.remap(5f, 0f, 10f, 100f, 200f), 1e-4);
        assertEquals(100f, Interpolation.remap(5f, 3f, 3f, 100f, 200f), 1e-4);
        assertEquals(0f, Interpolation.lerpAngle(0.1f, Angles.TWO_PI - 0.1f, 0.5f), 1e-5);
        assertEquals(1f, Interpolation.lerpAngle(1f, 1f, 0.7f), 1e-6);
        assertEquals(0.5f, Interpolation.lerpAngle(0f, 1f, 0.5f), 1e-6);
    }

    @Test void intMath() {
        assertEquals(16, PowersOfTwo.nextPowerOfTwo(9));
        assertEquals(3, PowersOfTwo.log2(8));
        assertEquals(0, PowersOfTwo.ceilLog2(1));
        assertEquals(1, PowersOfTwo.ceilLog2(2));
        assertEquals(2, PowersOfTwo.ceilLog2(3));
        assertEquals(10, PowersOfTwo.ceilLog2(1024));
        assertEquals(11, PowersOfTwo.ceilLog2(1025));
        assertEquals(30, PowersOfTwo.ceilLog2(1 << 30));
        assertEquals(31, PowersOfTwo.ceilLog2(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> PowersOfTwo.ceilLog2(0));
        assertThrows(IllegalArgumentException.class, () -> PowersOfTwo.ceilLog2(-4));
    }

    @Test void alignUp() {
        assertEquals(8, PowersOfTwo.alignUp(5, 4));
        assertEquals(8, PowersOfTwo.alignUp(8, 4));
        assertEquals(0, PowersOfTwo.alignUp(0, 16));
        assertEquals(-4, PowersOfTwo.alignUp(-5, 4));
        assertEquals(7, PowersOfTwo.alignUp(7, 1));
        assertThrows(IllegalArgumentException.class, () -> PowersOfTwo.alignUp(3, 3));
        assertThrows(IllegalArgumentException.class, () -> PowersOfTwo.alignUp(3, 0));
        assertThrows(ArithmeticException.class, () -> PowersOfTwo.alignUp(Integer.MAX_VALUE, 2));
    }

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
}
