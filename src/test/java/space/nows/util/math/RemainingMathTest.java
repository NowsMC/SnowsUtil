package space.nows.util.math;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RemainingMathTest {
    @Test
    void anglesConvertBothWaysAndRoundTrip() {
        assertEquals(Angles.PI, Angles.toRadians(180f), 1e-6f);
        assertEquals(90f, Angles.toDegrees((float) (Math.PI / 2)), 1e-4f);
        for (float d = -720f; d <= 720f; d += 13.7f) {
            assertEquals(d, Angles.toDegrees(Angles.toRadians(d)), 1e-3f);
        }
    }

    @Test
    void modWrapStaysInHalfOpenRange() {
        Random r = new Random(121);
        for (int i = 0; i < 50_000; i++) {
            float min = r.nextFloat() * 20 - 10;
            float max = min + 0.5f + r.nextFloat() * 20;
            float v = r.nextFloat() * 400 - 200;
            float w = Modulo.wrap(v, min, max);
            assertTrue(w >= min && w < max, "wrap(" + v + ") = " + w + " not in [" + min + ", " + max + ")");
        }
        assertEquals(0.5f, Modulo.wrap(4.5f, 0f, 4f), 1e-6f);
        assertThrows(IllegalArgumentException.class, () -> Modulo.wrap(1f, 2f, 2f));
    }

    @Test
    void floorModIntMatchesMathFloorMod() {
        for (int a = -50; a <= 50; a++) {
            for (int m = 1; m <= 9; m++) {
                assertEquals(Math.floorMod(a, m), Modulo.mod(a, m));
            }
        }
    }

    @Test
    void smootherstepAndSmoothstepHitEndpointsAndAreMonotone() {
        float prevA = -1f, prevB = -1f;
        for (int i = 0; i <= 1000; i++) {
            float x = i / 1000f;
            float a = Interpolation.smoothstep(0f, 1f, x);
            float b = Interpolation.smootherstep(0f, 1f, x);
            assertTrue(a >= prevA && b >= prevB - 1e-6f, "monotone up to float rounding at " + x);
            prevA = a;
            prevB = b;
        }
        assertEquals(0f, Interpolation.smoothstep(0f, 1f, 0f));
        assertEquals(1f, Interpolation.smootherstep(0f, 1f, 1f));
        assertEquals(0f, Interpolation.smootherstep(2f, 2f, 1f), "degenerate edges step at edge0");
        assertEquals(1f, Interpolation.smoothstep(2f, 2f, 3f));
    }

    @Test
    void remapInvertsItselfAndHandlesDegenerateInput() {
        assertEquals(50f, Interpolation.remap(5f, 0f, 10f, 0f, 100f), 1e-4f);
        assertEquals(-10f, Interpolation.remap(0f, 0f, 10f, -10f, 10f), 1e-4f);
        assertEquals(7f, Interpolation.remap(3f, 4f, 4f, 7f, 9f), 1e-6f, "degenerate input range yields outMin");
    }

    @Test
    void lerpAngleTakesTheShortArc() {
        float nearZero = Interpolation.lerpAngle(0.1f, -0.1f, 0.5f);
        assertEquals(0f, nearZero, 1e-5f, "short arc between 0.1 and -0.1 passes through 0");
        float wrapped = Interpolation.lerpAngle(Angles.TWO_PI - 0.1f, 0.1f, 0.5f);
        assertEquals(Angles.TWO_PI, wrapped, 1e-4f, "short arc across the 2pi seam");
        assertEquals(1.0f, Interpolation.lerpAngle(0f, 2f, 0.5f), 1e-6f);
    }

    @Test
    void fastTrigIsCloseToTheJdkAcrossRanges() {
        Random r = new Random(122);
        float maxSin = 0f, maxCos = 0f, maxSinSmooth = 0f, maxCosSmooth = 0f;
        for (int i = 0; i < 100_000; i++) {
            float x = (r.nextFloat() * 2 - 1) * 100f;
            maxSin = Math.max(maxSin, Math.abs(FastTrig.sin(x) - (float) Math.sin(x)));
            maxCos = Math.max(maxCos, Math.abs(FastTrig.cos(x) - (float) Math.cos(x)));
            maxSinSmooth = Math.max(maxSinSmooth, Math.abs(FastTrig.sinSmooth(x) - (float) Math.sin(x)));
            maxCosSmooth = Math.max(maxCosSmooth, Math.abs(FastTrig.cosSmooth(x) - (float) Math.cos(x)));
        }
        assertTrue(maxSinSmooth <= maxSin + 1e-6f, "interpolated sine must not be worse than the table");
        assertTrue(maxCosSmooth <= maxCos + 1e-6f, "interpolated cosine must not be worse than the table");
        assertTrue(maxSinSmooth < 1e-3f, "smooth sine error " + maxSinSmooth);
        assertTrue(maxCosSmooth < 1e-3f, "smooth cosine error " + maxCosSmooth);
    }
}
