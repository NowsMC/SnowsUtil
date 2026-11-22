package space.nows.util.check;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreconditionsTest {
    @Test void notNullNamesTheParameter() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> Preconditions.notNull(null, "buffer"));
        assertTrue(e.getMessage().contains("buffer"));
        assertEquals("x", Preconditions.notNull("x", "s"));
    }

    @Test void indexAndPosition() {
        assertEquals(3, Preconditions.index(3, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.index(4, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.index(-1, 4));
        assertEquals(4, Preconditions.position(4, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.position(5, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.position(-1, 4));
    }

    @Test void rangeIsOverflowSafe() {
        Preconditions.range(2, 2, 4);
        Preconditions.range(4, 0, 4);
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.range(3, 2, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.range(-1, 1, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.range(0, -1, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.range(Integer.MAX_VALUE, 2, Integer.MAX_VALUE));
    }

    @Test void numbers() {
        assertEquals(0, Preconditions.nonNegative(0, "n"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Preconditions.nonNegative(-1, "count"));
        assertTrue(e.getMessage().contains("count"));
        assertEquals(1, Preconditions.positive(1, "n"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.positive(0, "n"));
        assertEquals(1.5f, Preconditions.finite(1.5f, "f"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.finite(Float.NaN, "f"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.finite(Float.POSITIVE_INFINITY, "f"));
    }

    @Test void conditionsAndStrings() {
        Preconditions.argument(true, "never");
        Preconditions.state(true, "never");
        assertThrows(IllegalArgumentException.class, () -> Preconditions.argument(false, "bad"));
        assertThrows(IllegalStateException.class, () -> Preconditions.state(false, "bad"));
        assertEquals("a", Preconditions.notEmpty("a", "s"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.notEmpty("", "s"));
        assertThrows(NullPointerException.class, () -> Preconditions.notEmpty(null, "s"));
        assertEquals(" a ", Preconditions.notBlank(" a ", "s"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.notBlank(" \t", "s"));
    }
}
