package space.nows.util.json;

import org.junit.jupiter.api.Test;
import space.nows.util.error.JsonException;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JsonWriterTest {
    private static JsonObject sample() {
        return new JsonObject()
                .put("quote", "\"\\\n\t")
                .put("items", new JsonArray().add(1).addNull().add(true));
    }

    @Test
    void writesCompactJson() {
        assertEquals("{\"quote\":\"\\\"\\\\\\n\\t\",\"items\":[1,null,true]}", Json.write(sample()));
    }

    @Test
    void writesIndentedJsonThatRoundTrips() {
        JsonObject object = sample();
        String pretty = Json.writePretty(object);

        assertTrue(pretty.contains("\n  \"quote\": "));
        assertEquals(object, Json.parse(pretty));
        assertEquals("{}", Json.writePretty(new JsonObject()));
        assertEquals("[]", Json.writePretty(new JsonArray()));
    }

    @Test
    void writesToAppendableAndStreams() throws Exception {
        JsonObject object = sample();

        StringWriter writer = new StringWriter();
        Json.write(object, writer);
        assertEquals(Json.write(object), writer.toString());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Json.writePretty(object, output);
        assertEquals(Json.writePretty(object), output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void escapesControlCharactersAndUnpairedSurrogates() {
        assertEquals("\"\\u0001\\u001f\"", Json.write(new JsonString("\u0001\u001F")));
        assertEquals("\"\\ud83d\"", Json.write(new JsonString("\uD83D")));
        assertEquals("\"😀\"", Json.write(new JsonString("😀")));
    }

    @Test
    void enforcesNestingLimitWhenWriting() {
        JsonValue atLimit = nested(Json.MAX_DEPTH);
        assertEquals(atLimit, Json.parse(Json.write(atLimit)));

        JsonValue tooDeep = nested(Json.MAX_DEPTH + 1);
        assertThrows(JsonException.class, () -> Json.write(tooDeep));
    }

    private static JsonValue nested(int levels) {
        JsonValue value = JsonNull.INSTANCE;
        for (int i = 0; i < levels; i++) value = new JsonArray().add(value);
        return value;
    }
}
