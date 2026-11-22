package space.nows.util.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FastTrigSmoothTest {
    @Test void smoothVariantsAreMoreAccurateThanTableLookup() {
        double worstSmooth = 0;
        double worstTable = 0;
        for (float x = -50f; x <= 50f; x += 0.0137f) {
            worstSmooth = Math.max(worstSmooth, Math.abs(FastTrig.sinSmooth(x) - Math.sin(x)));
            worstSmooth = Math.max(worstSmooth, Math.abs(FastTrig.cosSmooth(x) - Math.cos(x)));
            worstTable = Math.max(worstTable, Math.abs(FastTrig.sin(x) - Math.sin(x)));
        }
        assertTrue(worstSmooth < 1e-6, "smooth error " + worstSmooth);
        assertTrue(worstSmooth < worstTable);
    }

    @Test void smoothRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> FastTrig.sinSmooth(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> FastTrig.cosSmooth(Float.POSITIVE_INFINITY));
        assertEquals(0f, FastTrig.sinSmooth(0f), 1e-7f);
        assertEquals(1f, FastTrig.cosSmooth(0f), 1e-7f);
    }
}
