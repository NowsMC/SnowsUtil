package space.nows.util.text;

import org.junit.jupiter.api.Test;
import space.nows.util.error.InvalidHexException;

import java.util.HexFormat;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TextTest {
    // ---------------------------------------------------------------- NumberParser
    private static long jdkLong(String s, long fallback) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static int jdkInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    @Test void parsersAgreeWithTheJdkOnAsciiInput() {
        String alphabet = "0123456789+- x";
        Random r = new Random(41);
        for (int i = 0; i < 200_000; i++) {
            int len = r.nextInt(23);
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < len; k++) sb.append(alphabet.charAt(r.nextInt(r.nextInt(8) == 0 ? alphabet.length() : 12)));
            String s = sb.toString();
            assertEquals(jdkLong(s, -77L), NumberParser.parseLong(s, -77L));
            assertEquals(jdkInt(s, -77), NumberParser.parseInt(s, -77));
        }
    }

    @Test void parserEdges() {
        assertEquals(Long.MAX_VALUE, NumberParser.parseLong("9223372036854775807", 0));
        assertEquals(Long.MIN_VALUE, NumberParser.parseLong("-9223372036854775808", 0));
        assertEquals(-1L, NumberParser.parseLong("9223372036854775808", -1L));
        assertEquals(-1L, NumberParser.parseLong("-9223372036854775809", -1L));
        assertEquals(5L, NumberParser.parseLong("+5", 0));
        assertEquals(0L, NumberParser.parseLong("-0", 9));
        assertEquals(Integer.MIN_VALUE, NumberParser.parseInt("-2147483648", 0));
        assertEquals(-1, NumberParser.parseInt("2147483648", -1));
        assertEquals(-1, NumberParser.parseInt("99999999999", -1));
        for (String bad : new String[]{"", "-", "+", " 1", "1 ", "1_000", "0x10", "1e3", "--1"}) {
            assertEquals(-5, NumberParser.parseInt(bad, -5), bad);
            assertEquals(-5L, NumberParser.parseLong(bad, -5L), bad);
        }
        assertEquals(-5, NumberParser.parseInt(null, -5));
        assertEquals(-5L, NumberParser.parseLong(null, -5L));
        assertEquals(-5, NumberParser.parseInt("\u0661\u0662", -5));          // Arabic-Indic digits are not ASCII digits
        assertEquals(-5L, NumberParser.parseLong("\uFF11\uFF12", -5L));        // neither are full-width digits
    }

    @Test void booleanAndDoubleParsing() {
        assertTrue(NumberParser.parseBoolean("TRUE", false));
        assertFalse(NumberParser.parseBoolean("False", true));
        assertTrue(NumberParser.parseBoolean("yes", true));
        assertFalse(NumberParser.parseBoolean(null, false));
        assertEquals(1.5, NumberParser.parseDouble("1.5", 0));
        assertEquals(1000.0, NumberParser.parseDouble("1e3", 0));
        assertEquals(-1.0, NumberParser.parseDouble("abc", -1));
        assertEquals(-1.0, NumberParser.parseDouble("", -1));
        assertEquals(-1.0, NumberParser.parseDouble(null, -1));
    }

    // ---------------------------------------------------------------- Hex
    @Test void hexMatchesTheJdkAndRoundTrips() {
        Random r = new Random(42);
        HexFormat jdk = HexFormat.of();
        for (int i = 0; i < 2_000; i++) {
            byte[] data = new byte[r.nextInt(64)];
            r.nextBytes(data);
            String hex = Hex.toHex(data);
            assertEquals(jdk.formatHex(data), hex);
            assertEquals(jdk.withUpperCase().formatHex(data), Hex.toUpperHex(data));
            assertArrayEquals(data, Hex.fromHex(hex));
            assertArrayEquals(data, Hex.fromHex(Hex.toUpperHex(data)));
            assertTrue(Hex.isHex(hex));
        }
        assertEquals("", Hex.toHex(new byte[0]));
        assertEquals(0, Hex.fromHex("").length);
    }

    @Test void hexRangeEncodingAndValidation() {
        byte[] data = {0, 1, (byte) 0xAB, (byte) 0xCD};
        assertEquals("01ab", Hex.toHex(data, 1, 2));
        assertEquals("", Hex.toHex(data, 4, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Hex.toHex(data, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Hex.toHex(data, -1, 1));
        assertThrows(NullPointerException.class, () -> Hex.toHex(null));
        assertThrows(NullPointerException.class, () -> Hex.fromHex(null));
    }

    @Test void malformedHexReportsTheOffendingIndex() {
        InvalidHexException odd = assertThrows(InvalidHexException.class, () -> Hex.fromHex("abc"));
        assertEquals(3, odd.index());
        InvalidHexException bad = assertThrows(InvalidHexException.class, () -> Hex.fromHex("0g"));
        assertEquals(1, bad.index());
        assertEquals(0, assertThrows(InvalidHexException.class, () -> Hex.fromHex("zz00")).index());
        assertEquals(2, assertThrows(InvalidHexException.class, () -> Hex.fromHex("00 1")).index());
        assertThrows(InvalidHexException.class, () -> Hex.fromHex("\uFF11\uFF12"));   // full-width digits are rejected
        assertThrows(IllegalArgumentException.class, () -> Hex.fromHex("xx"));        // still an IllegalArgumentException for old callers
        assertFalse(Hex.isHex("abc"));
        assertFalse(Hex.isHex("0g"));
        assertFalse(Hex.isHex(null));
        assertTrue(Hex.isHex(""));
        assertTrue(Hex.isHex("aBcD09"));
    }

    // ---------------------------------------------------------------- StringUtils
    @Test void splitMatchesTheJdkWithNegativeLimit() {
        for (String s : new String[]{"", "a", "a,b", "a,,b,", ",", ",,", ",a", "abc,def,ghi"}) {
            assertEquals(List.of(s.split(",", -1)), StringUtils.split(s, ','), "input: " + s);
        }
        assertThrows(NullPointerException.class, () -> StringUtils.split(null, ','));
        assertEquals(2, StringUtils.count("a,b,c", ','));
        assertEquals(0, StringUtils.count("", ','));
        assertThrows(NullPointerException.class, () -> StringUtils.count(null, ','));
    }

    @Test void blankEmptyAndDefaults() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty(" "));
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank("  \t\n"));
        assertFalse(StringUtils.isBlank(" a "));
        assertEquals("fb", StringUtils.defaultIfBlank("  ", "fb"));
        assertEquals("x", StringUtils.defaultIfBlank("x", "fb"));
        assertEquals("fb", StringUtils.defaultIfBlank(null, "fb"));
    }

    @Test void paddingAndCapitalize() {
        assertEquals("007", StringUtils.padLeft("7", 3, '0'));
        assertEquals("7  ", StringUtils.padRight("7", 3, ' '));
        assertEquals("long", StringUtils.padLeft("long", 2, '0'));
        assertEquals("abc", StringUtils.padRight("abc", -5, ' '));
        assertThrows(NullPointerException.class, () -> StringUtils.padLeft(null, 3, '0'));
        assertThrows(NullPointerException.class, () -> StringUtils.padRight(null, 3, '0'));
        assertEquals("Hello", StringUtils.capitalize("hello"));
        assertEquals("Hello", StringUtils.capitalize("Hello"));
        assertEquals("", StringUtils.capitalize(""));
        assertNull(StringUtils.capitalize(null));
    }

    @Test void truncateNeverSplitsASurrogatePair() {
        String s = "a\uD83D\uDE00b";                                // a, a 2-char emoji, b
        assertEquals("a", StringUtils.truncate(s, 2));
        assertEquals("a\uD83D\uDE00", StringUtils.truncate(s, 3));
        assertEquals(s, StringUtils.truncate(s, 4));
        assertEquals(s, StringUtils.truncate(s, 100));
        assertEquals("", StringUtils.truncate(s, 0));
        assertEquals("", StringUtils.truncate("\uD83D\uDE00", 1));
        assertEquals("abc", StringUtils.truncate("abcdef", 3));
        assertThrows(IllegalArgumentException.class, () -> StringUtils.truncate("abc", -1));
        assertThrows(NullPointerException.class, () -> StringUtils.truncate(null, 1));
    }
}
