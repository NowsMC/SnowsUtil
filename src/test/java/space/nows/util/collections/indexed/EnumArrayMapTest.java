package space.nows.util.collections.indexed;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumArrayMapTest {
    enum Color { RED, GREEN, BLUE }

    @Test void presenceAndSize() {
        EnumArrayMap<Color, String> e = new EnumArrayMap<>(Color.class);
        e.put(Color.GREEN, "g");
        assertTrue(e.containsKey(Color.GREEN));
        assertFalse(e.containsKey(Color.RED));
        assertEquals(1, e.size());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test void bridgesToTheJdkAndCopiesIndependently() {
        EnumArrayMap<Color, Integer> m = new EnumArrayMap<>(Color.class);
        m.put(Color.BLUE, 3);
        m.put(Color.RED, 1);
        EnumMap<Color, Integer> jdk = m.toEnumMap();
        assertEquals(Map.of(Color.RED, 1, Color.BLUE, 3), jdk);
        EnumArrayMap<Color, Integer> c = m.copy();
        m.put(Color.GREEN, 2);
        assertEquals(2, c.size());
        assertEquals("{RED=1, BLUE=3}", c.toString());
        assertThrows(NullPointerException.class, () -> new EnumArrayMap<Color, Integer>(null));
        assertThrows(IllegalArgumentException.class, () -> new EnumArrayMap(String.class));
    }
}
