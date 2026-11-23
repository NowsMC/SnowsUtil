package space.nows.util.json;

import org.junit.jupiter.api.Test;
import space.nows.util.data.Arr;
import space.nows.util.data.Obj;
import space.nows.util.data.Str;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class JsonWriteOptionsTest {
    @Test
    void defaultOutputEscapesOnlyWhatTheSpecRequires() {
        assertEquals("\"<script>&\"", Json.write(new Str("<script>&")));
        assertEquals("\"สวัสดี\"", Json.write(new Str("สวัสดี")));
    }

    @Test
    void htmlEscapingWritesAngleBracketsAndAmpersandAsHexEscapes() {
        WriteOptions html = WriteOptions.builder().escapeHtml(true).build();
        assertEquals("\"\\u003cscript\\u003e\\u0026\\u003c/script\\u003e\"", Json.write(new Str("<script>&</script>"), html));
        assertEquals("\"<safe>\"", Json.write(new Str("<safe>"), WriteOptions.COMPACT), "off by default");
    }

    @Test
    void asciiOnlyEscapesEveryNonAsciiCodeUnit() {
        WriteOptions ascii = WriteOptions.builder().asciiOnly(true).build();
        String out = Json.write(new Str("สวัสดี 😀"), ascii);
        for (char c : out.toCharArray()) assertTrue(c <= 0x7F, "non-ASCII in " + out);
        assertTrue(out.contains("\\ud83d\\ude00"), out);
        assertEquals("สวัสดี 😀", Json.parse(out).asString());
    }

    @Test
    void optionsRoundTripThroughTheParser() {
        Obj doc = new Obj().put("text", "a<b>&c\"d\n").put("list", new Arr().add("é").add(1L));
        WriteOptions everything = WriteOptions.builder().escapeHtml(true).asciiOnly(true).build();
        assertEquals(doc, Json.parse(Json.write(doc, everything)));
    }

    @Test
    void prettyOptionMatchesWritePretty() {
        Obj doc = new Obj().put("a", 1L).put("b", new Arr().add(true));
        assertEquals(Json.writePretty(doc), Json.write(doc, WriteOptions.PRETTY));
        assertEquals(Json.write(doc), Json.write(doc, WriteOptions.COMPACT));
    }

    @Test
    void streamingOverloadWritesTheSameText() throws Exception {
        WriteOptions html = WriteOptions.builder().escapeHtml(true).build();
        StringWriter out = new StringWriter();
        Json.write(new Str("<x>"), html, out);
        assertEquals(Json.write(new Str("<x>"), html), out.toString());
    }
}
