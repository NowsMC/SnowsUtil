package space.nows.util.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorsTest {
    @Test void notNullNamesTheParameter() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> Checks.notNull(null, "buffer"));
        assertTrue(e.getMessage().contains("buffer"));
        assertEquals("x", Checks.notNull("x", "s"));
    }

    @Test void indexAndPosition() {
        assertEquals(3, Checks.index(3, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Checks.index(4, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Checks.index(-1, 4));
        assertEquals(4, Checks.position(4, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Checks.position(5, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Checks.position(-1, 4));
    }

    @Test void rangeIsOverflowSafe() {
        Checks.range(2, 2, 4);
        Checks.range(4, 0, 4);
        assertThrows(IndexOutOfBoundsException.class, () -> Checks.range(3, 2, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Checks.range(-1, 1, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Checks.range(0, -1, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Checks.range(Integer.MAX_VALUE, 2, Integer.MAX_VALUE));
    }

    @Test void numbers() {
        assertEquals(0, Checks.nonNegative(0, "n"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Checks.nonNegative(-1, "count"));
        assertTrue(e.getMessage().contains("count"));
        assertEquals(1, Checks.positive(1, "n"));
        assertThrows(IllegalArgumentException.class, () -> Checks.positive(0, "n"));
        assertEquals(1.5f, Checks.finite(1.5f, "f"));
        assertThrows(IllegalArgumentException.class, () -> Checks.finite(Float.NaN, "f"));
        assertThrows(IllegalArgumentException.class, () -> Checks.finite(Float.POSITIVE_INFINITY, "f"));
    }

    @Test void conditionsAndStrings() {
        Checks.argument(true, "never");
        Checks.state(true, "never");
        assertThrows(IllegalArgumentException.class, () -> Checks.argument(false, "bad"));
        assertThrows(IllegalStateException.class, () -> Checks.state(false, "bad"));
        assertEquals("a", Checks.notEmpty("a", "s"));
        assertThrows(IllegalArgumentException.class, () -> Checks.notEmpty("", "s"));
        assertThrows(NullPointerException.class, () -> Checks.notEmpty(null, "s"));
        assertEquals(" a ", Checks.notBlank(" a ", "s"));
        assertThrows(IllegalArgumentException.class, () -> Checks.notBlank(" \t", "s"));
    }

    @Test void exceptionsCarryDetailAndKeepTheirJdkSuperclass() {
        CapacityExceededException c = new CapacityExceededException("buf", 10, 5);
        assertEquals(10, c.requested());
        assertEquals(5, c.limit());
        assertTrue(c.getMessage().contains("buf"));
        assertTrue(((Object) c) instanceof IllegalStateException);

        InsufficientDataException i = new InsufficientDataException(8, 3);
        assertEquals(8, i.needed());
        assertEquals(3, i.available());
        assertTrue(((Object) i) instanceof IndexOutOfBoundsException);

        KeyOutOfRangeException k = new KeyOutOfRangeException(9, 0, 5);
        assertEquals(9, k.key());
        assertEquals(0, k.min());
        assertEquals(5, k.max());
        assertTrue(((Object) k) instanceof IndexOutOfBoundsException);

        InvalidHexException h = new InvalidHexException("bad", 7);
        assertEquals(7, h.index());
        assertTrue(h.getMessage().contains("index 7"));
        assertTrue(((Object) h) instanceof IllegalArgumentException);

        assertTrue(((Object) new SingularMatrixException("x")) instanceof ArithmeticException);
    }
}
