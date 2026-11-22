package space.nows.util.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionsTest {
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
