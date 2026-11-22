package space.nows.util.math;

import org.junit.jupiter.api.Test;
import space.nows.util.math.linear.Quaternion;
import space.nows.util.text.Strings;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DefinitionTest {
    @Test void lerpIsExactAtBothEnds() {
        Random r = new Random(31);
        for (int i = 0; i < 100_000; i++) {
            float a = (r.nextFloat() - 0.5f) * 1e6f, b = (r.nextFloat() - 0.5f) * 1e-3f;
            assertEquals(a, Interpolation.lerp(a, b, 0f));
            assertEquals(b, Interpolation.lerp(a, b, 1f));
            double c = (r.nextDouble() - 0.5) * 1e9, d = (r.nextDouble() - 0.5) * 1e-6;
            assertEquals(c, Interpolation.lerp(c, d, 0.0));
            assertEquals(d, Interpolation.lerp(c, d, 1.0));
        }
        assertEquals(5f, Interpolation.lerp(0f, 10f, 0.5f));
        assertEquals(15f, Interpolation.lerp(10f, 20f, 0.5f));
    }

    @Test void lerpIsMonotonic() {
        float a = 0.1f, b = 1e7f;
        float prev = Interpolation.lerp(a, b, 0f);
        for (int i = 1; i <= 10_000; i++) {
            float v = Interpolation.lerp(a, b, i / 10_000f);
            assertTrue(v >= prev, "t index " + i);
            prev = v;
        }
    }

    @Test void wrapStaysInsideTheHalfOpenRange() {
        Random r = new Random(32);
        for (int i = 0; i < 200_000; i++) {
            float min = (r.nextFloat() - 0.5f) * 1e4f;
            float max = min + 0.001f + r.nextFloat() * 100f;
            float v = (r.nextFloat() - 0.5f) * 1e5f;
            float w = Modulo.wrap(v, min, max);
            assertTrue(w >= min && w < max, min + " " + max + " " + v + " -> " + w);
        }
        assertEquals(0f, Modulo.wrap(5f, 0f, 5f));
        assertEquals(1f, Modulo.wrap(4f, 0f, 3f));
    }

    @Test void capitalizeUsesTitleCaseOfTheFirstCodePoint() {
        assertEquals("Hello", Strings.capitalize("hello"));
        assertEquals("Hello", Strings.capitalize("Hello"));
        assertEquals("ǅx", Strings.capitalize("ǆx"));
        assertEquals("𐐀a", Strings.capitalize("𐐨a"));
        assertEquals("", Strings.capitalize(""));
        assertNull(Strings.capitalize(null));
        assertEquals("1a", Strings.capitalize("1a"));
    }

    @Test void axisAngleQuaternionsAreUnitLengthForAnyAxisScale() {
        Quaternion q = new Quaternion().setFromAxisAngle(0f, 0f, 5f, Angles.HALF_PI);
        assertEquals(1f, q.length(), 1e-6f);
        Quaternion unit = new Quaternion().setFromAxisAngle(0f, 0f, 1f, Angles.HALF_PI);
        assertEquals(unit, q);
        assertThrows(IllegalArgumentException.class, () -> new Quaternion().setFromAxisAngle(0f, 0f, 0f, 1f));
        assertThrows(IllegalArgumentException.class, () -> new Quaternion().setFromAxisAngle(Float.NaN, 0f, 1f, 1f));
    }
}
