package space.nows.util.json;

import org.junit.jupiter.api.Test;
import space.nows.util.error.JsonException;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JsonParserTest {
    @Test
    void parsesStringsNumbersAndNestedValues() {
        JsonObject object = Json.parse("""
                {"name":"สวัสดี","escaped":"line\\n\\uD83D\\uDE00","count":42,"large":123456789012345678901234567890}
                """).asObject();

        assertEquals("สวัสดี", object.getString("name"));
        assertEquals("line\n😀", object.getString("escaped"));
        assertEquals(42, object.getInt("count"));
        assertEquals(new BigDecimal("123456789012345678901234567890"), object.getNumber("large").asDecimal());
    }

    @Test
    void keepsExponentFormsAndSkipsWhitespace() {
        assertEquals(new BigDecimal("1E+3"), Json.parse("1E+3").asNumber().asDecimal());
        assertEquals(new BigDecimal("-0.25"), Json.parse("-0.25").asNumber().asDecimal());
        assertEquals(2, Json.parse(" \t\r\n[ 1 , 2 ]\n ").asArray().size());
    }

    @Test
    void readsUtf8FromBytesStreamsAndReaders() {
        JsonObject object = new JsonObject().put("text", "สวัสดี");
        String json = Json.write(object);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        assertEquals(object, Json.parse(bytes));
        assertEquals(object, Json.parse(new ByteArrayInputStream(bytes)));
        assertEquals(object, Json.parse(new StringReader(json)));
        assertThrows(JsonException.class, () -> Json.parse(new byte[]{'"', (byte) 0xC3, '"'}));
    }

    @Test
    void rejectsMalformedDocuments() {
        String[] malformed = {
                "{\"a\":1,\"a\":2}", "[1,]", "{\"a\":1,}", "{1:2}", "01", "-", "1.", "1e", "",
                "nul", "[true] trailing", "\"control\ncharacter\"", "\"\\uZZZZ\"", "\"\\ud83d\"", "\"\\ude00\"",
                "\"\\u\uFF10\uFF10\uFF10\uFF10\"",
        };
        for (String input : malformed) {
            assertThrows(JsonException.class, () -> Json.parse(input), input);
        }
    }

    @Test
    void enforcesNestingLimit() {
        assertDoesNotThrow(() -> Json.parse("[".repeat(Json.MAX_DEPTH) + "0" + "]".repeat(Json.MAX_DEPTH)));
        assertThrows(JsonException.class, () -> Json.parse("[".repeat(Json.MAX_DEPTH + 1) + "0" + "]".repeat(Json.MAX_DEPTH + 1)));
    }
}
