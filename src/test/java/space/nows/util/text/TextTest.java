package space.nows.util.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextTest {
    @Test void numberParser() {
        assertEquals(123, NumberParser.parseInt("123", -1));
        assertEquals(-9, NumberParser.parseInt("-9", 0));
        assertEquals(-1, NumberParser.parseInt("12x", -1));
        assertEquals(-1, NumberParser.parseInt("99999999999", -1));
        assertEquals(Integer.MIN_VALUE, NumberParser.parseInt("-2147483648", 0));
    }

    @Test void hex() {
        assertEquals("00ff", Hex.toHex(new byte[]{0, (byte) 0xFF}));
        assertArrayEquals(new byte[]{0, (byte) 0xFF}, Hex.fromHex("00FF"));
    }

    @Test void stringUtils() {
        assertEquals(3, StringUtils.split("a,,b", ',').size());
        assertEquals("007", StringUtils.padLeft("7", 3, '0'));
        assertTrue(StringUtils.isBlank("  \t"));
    }
}
