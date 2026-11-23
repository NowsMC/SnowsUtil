package space.nows.util.json;

import org.junit.jupiter.api.Test;
import space.nows.util.data.*;
import space.nows.util.error.DataException;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JsonWriterTest {
    private static DataObject sample() {
        return new DataObject()
                .put("quote", "\"\\\n\t")
                .put("items", new DataArray().add(1).addNull().add(true));
    }

    @Test
    void writesCompactJson() {
        assertEquals("{\"quote\":\"\\\"\\\\\\n\\t\",\"items\":[1,null,true]}", Json.write(sample()));
    }

    @Test
    void writesIndentedJsonThatRoundTrips() {
        DataObject object = sample();
        String pretty = Json.writePretty(object);

        assertTrue(pretty.contains("\n  \"quote\": "));
        assertEquals(object, Json.parse(pretty));
        assertEquals("{}", Json.writePretty(new DataObject()));
        assertEquals("[]", Json.writePretty(new DataArray()));
    }

    @Test
    void writesToAppendableAndStreams() throws Exception {
        DataObject object = sample();

        StringWriter writer = new StringWriter();
        Json.write(object, writer);
        assertEquals(Json.write(object), writer.toString());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Json.writePretty(object, output);
        assertEquals(Json.writePretty(object), output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void escapesControlCharactersAndUnpairedSurrogates() {
        assertEquals("\"\\u0001\\u001f\"", Json.write(new DataString("\u0001\u001F")));
        assertEquals("\"\\ud83d\"", Json.write(new DataString("\uD83D")));
        assertEquals("\"😀\"", Json.write(new DataString("😀")));
    }

    @Test
    void enforcesNestingLimitWhenWriting() {
        DataValue atLimit = nested(DataValue.MAX_DEPTH);
        assertEquals(atLimit, Json.parse(Json.write(atLimit)));

        DataValue tooDeep = nested(DataValue.MAX_DEPTH + 1);
        assertThrows(DataException.class, () -> Json.write(tooDeep));
    }

    private static DataValue nested(int levels) {
        DataValue value = DataNull.INSTANCE;
        for (int i = 0; i < levels; i++) value = new DataArray().add(value);
        return value;
    }
}
