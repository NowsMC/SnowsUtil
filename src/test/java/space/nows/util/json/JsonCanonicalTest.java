package space.nows.util.json;

import org.junit.jupiter.api.Test;
import space.nows.util.data.Num;
import space.nows.util.data.Obj;
import space.nows.util.data.Value;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class JsonCanonicalTest {
    @Test
    void numbersFollowEcmaScriptFormatting() {
        assertEquals("0.000001", Canonical.formatNumber(0.000001));
        assertEquals("1e-7", Canonical.formatNumber(1e-7));
        assertEquals("1e+21", Canonical.formatNumber(1e21));
        assertEquals("123456789012345680000", Canonical.formatNumber(1.2345678901234568e20));
        assertEquals("0.1", Canonical.formatNumber(0.1));
        assertEquals("5e-324", Canonical.formatNumber(Double.MIN_VALUE));
        assertEquals("1.7976931348623157e+308", Canonical.formatNumber(Double.MAX_VALUE));
        assertEquals("4.5", Canonical.formatNumber(4.5));
        assertEquals("333333333.3333333", Canonical.formatNumber(333333333.33333329));
        assertEquals("0", Canonical.formatNumber(-0.0));
        assertEquals("-1.5", Canonical.formatNumber(-1.5));
        assertEquals("100", Canonical.formatNumber(100.0));
    }

    @Test
    void membersAreSortedByUtf16CodeUnits() {
        Obj doc = new Obj()
                .put("\u20ac", "Euro Sign")
                .put("\r", "Carriage Return")
                .put("\ufb33", "Hebrew Letter Dalet With Dagesh")
                .put("1", "One")
                .put("\ud83d\ude00", "Emoji")
                .put("\u0080", "Control")
                .put("\u00f6", "Latin Small Letter O With Diaeresis");
        String expected = "{\"\\r\":\"Carriage Return\",\"1\":\"One\",\"\u0080\":\"Control\","
                + "\"\u00f6\":\"Latin Small Letter O With Diaeresis\",\"\u20ac\":\"Euro Sign\","
                + "\"\ud83d\ude00\":\"Emoji\",\"\ufb33\":\"Hebrew Letter Dalet With Dagesh\"}";
        assertEquals(expected, Json.canonical(doc));
    }

    @Test
    void canonicalFormIgnoresWhitespaceAndInputOrder() {
        Value a = Json.parse("{ \"b\" : [1.0, 2e0], \"a\" : null }");
        Value b = Json.parse("{\"a\":null,\"b\":[1,2]}");
        assertEquals(Json.canonical(a), Json.canonical(b));
        assertEquals("{\"a\":null,\"b\":[1,2]}", Json.canonical(a));
    }

    @Test
    void stringsEscapeOnlyWhatTheSpecRequires() {
        String input = "\"\\u0001\\b\\t\\n\\f\\r\\\"\\\\/\"";
        String expected = "\"\\u0001\\b\\t\\n\\f\\r\\\"\\\\/\"";
        assertEquals(expected, Json.canonical(Json.parse(input)));
        assertEquals("\"\u007f\"", Json.canonical(Json.parse("\"\u007f\"")), "DEL is literal");
    }

    @Test
    void canonicalNumbersUseTheDoubleValue() {
        assertEquals("0.1", Json.canonical(Num.of(new BigDecimal("0.10"))));
    }

    @Test
    void canonicalOutputParsesBackToAnEqualValue() {
        Value doc = Json.parse("{\"x\":[true,false,null,\"s\",-12.5e3],\"y\":{}}");
        assertEquals(doc, Json.parse(Json.canonical(doc)));
    }
}
