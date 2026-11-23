package space.nows.util.json;

import org.junit.jupiter.api.Test;
import space.nows.util.data.Num;
import space.nows.util.error.DataException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JsonLenientAndEncodingTest {
    private static final ParseOptions L = ParseOptions.lenient();

    @Test
    void lenientCommentsTrailingCommasAndQuotes() {
        assertEquals(1, Json.parse("{ /* hi */ \"a\": 1 // tail\n }", L).asObject().getLong("a"));
        assertEquals(3, Json.parse("[1,2,3,]", L).asArray().size());
        assertEquals(1, Json.parse("{\"a\":1,}", L).asObject().size());
        assertEquals("hi", Json.parse("'hi'", L).asString());
        assertEquals("it's", Json.parse("'it\\'s'", L).asString());
        assertEquals(1, Json.parse("{a:1, b_2:2}", L).asObject().getLong("a"));
        assertEquals(42, Json.parse("+42", L).asNumber().asInt());
        assertEquals(7, Json.parse("007", L).asNumber().asInt());
        assertEquals(255, Json.parse("0xFF", L).asNumber().asInt());
        assertEquals(255, Json.parse("0XfF", L).asNumber().asInt());
        assertEquals(-16, Json.parse("-0x10", L).asNumber().asInt());
    }

    @Test
    void nonFiniteNumbersAreNotRepresentableSoTheyAreRejectedClearly() {
        assertThrows(DataException.class, () -> Json.parse("NaN", L));
        assertThrows(DataException.class, () -> Json.parse("-Infinity", L));
    }

    @Test
    void strictRejectsEveryLeniency() {
        for (String bad : new String[]{"07", "[1,2,]", "{\"a\":1,}", "/* c */1", "{a:1}", "'x'", "+1", "0x1F", "{\"a\":1 // x\n}"}) {
            assertThrows(DataException.class, () -> Json.parse(bad), bad);
        }
    }

    @Test
    void duplicateKeysAreRejectedEvenWhenLenient() {
        assertThrows(DataException.class, () -> Json.parse("{\"a\":1,\"a\":2}", L));
        assertThrows(DataException.class, () -> Json.parse("{a:1,'a':2}", L));
    }

    @Test
    void unterminatedCommentsAndBadSlashesFail() {
        assertThrows(DataException.class, () -> Json.parse("/* open", L));
        assertThrows(DataException.class, () -> Json.parse("/x", L));
        assertThrows(DataException.class, () -> Json.parse("0x", L));
    }

    @Test
    void lenientDepthLimitIsConfigurable() {
        ParseOptions shallow = ParseOptions.builder().maxDepth(3).build();
        assertDoesNotThrow(() -> Json.parse("[[[1]]]", shallow));
        assertThrows(DataException.class, () -> Json.parse("[[[[1]]]]", shallow));
        assertThrows(DataException.class, () -> ParseOptions.builder().maxDepth(0));
        assertThrows(DataException.class, () -> ParseOptions.builder().maxDepth(10_000));
    }

    @Test
    void utf8WithAndWithoutBom() {
        byte[] plain = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        byte[] bom = concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, plain);
        assertEquals(1, Json.parseAuto(plain).asObject().getLong("a"));
        assertEquals(1, Json.parseAuto(bom).asObject().getLong("a"));
        assertEquals(1, Json.parseAuto(new ByteArrayInputStream(bom)).asObject().getLong("a"));
    }

    @Test
    void utf16BigAndLittleEndianWithBomAndWithoutBom() {
        String json = "{\"a\":2}";
        byte[] be = json.getBytes(StandardCharsets.UTF_16BE);
        byte[] le = json.getBytes(StandardCharsets.UTF_16LE);
        byte[] beBom = concat(new byte[]{(byte) 0xFE, (byte) 0xFF}, be);
        byte[] leBom = concat(new byte[]{(byte) 0xFF, (byte) 0xFE}, le);
        for (byte[] input : new byte[][]{be, le, beBom, leBom}) {
            assertEquals(2, Json.parseAuto(input).asObject().getLong("a"));
            assertEquals(2, Json.parseAuto(new ByteArrayInputStream(input)).asObject().getLong("a"));
        }
    }

    @Test
    void utf32BigAndLittleEndianWithBomAndWithoutBom() {
        String json = "{\"a\":3}";
        byte[] be = json.getBytes(java.nio.charset.Charset.forName("UTF-32BE"));
        byte[] le = json.getBytes(java.nio.charset.Charset.forName("UTF-32LE"));
        byte[] beBom = concat(new byte[]{0, 0, (byte) 0xFE, (byte) 0xFF}, be);
        byte[] leBom = concat(new byte[]{(byte) 0xFF, (byte) 0xFE, 0, 0}, le);
        for (byte[] input : new byte[][]{be, le, beBom, leBom}) {
            assertEquals(3, Json.parseAuto(input).asObject().getLong("a"));
            assertEquals(3, Json.parseAuto(new ByteArrayInputStream(input)).asObject().getLong("a"));
        }
    }

    @Test
    void malformedEncodedBytesAreReported() {
        assertThrows(DataException.class, () -> Json.parseAuto(new byte[]{'"', (byte) 0xC3, '"'}));
        assertThrows(DataException.class, () -> Json.parseAuto(new byte[]{(byte) 0xFF, (byte) 0xFE, 0x00}));
        assertThrows(DataException.class, () -> Json.parseAuto(new byte[0]));
    }

    @Test
    void errorsReportLineAndColumn() {
        DataException e = assertThrows(DataException.class, () -> Json.parse("{\n  \"a\": x}"));
        assertTrue(e.getMessage().contains("line 2"), e.getMessage());
        assertTrue(e.getMessage().contains("column"), e.getMessage());
    }

    @Test
    void negativeZeroSurvivesLenientForms() {
        assertEquals(Num.negativeZero().asDouble(), Json.parse("-0", L).asNumber().asDouble());
        assertEquals(new BigDecimal("0"), Json.parse("0x0", L).asNumber().asDecimal());
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
