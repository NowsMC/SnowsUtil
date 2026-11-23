package space.nows.util.json;

import org.junit.jupiter.api.Test;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JsonStandardsTest {
    private static final String RFC6901_DOC = """
            {"foo": ["bar", "baz"], "": 0, "a/b": 1, "c%d": 2, "e^f": 3, "g|h": 4, "i\\\\j": 5, "k\\"l": 6, " ": 7, "m~n": 8}
            """;

    @Test
    void pointerResolvesRfc6901Examples() {
        Value doc = Json.parse(RFC6901_DOC);
        assertEquals(doc, JsonPointer.root().get(doc));
        assertEquals("[\"bar\",\"baz\"]", Json.write(JsonPointer.parse("/foo").get(doc)));
        assertEquals("bar", JsonPointer.parse("/foo/0").get(doc).asString());
        assertEquals("0", Json.write(JsonPointer.parse("/").get(doc)));
        assertEquals("1", Json.write(JsonPointer.parse("/a~1b").get(doc)));
        assertEquals("8", Json.write(JsonPointer.parse("/m~0n").get(doc)));
        assertEquals("2", Json.write(JsonPointer.parse("/c%d").get(doc)));
        assertEquals("7", Json.write(JsonPointer.parse("/ ").get(doc)));
    }

    @Test
    void pointerMissingAndMalformedCases() {
        Value doc = Json.parse(RFC6901_DOC);
        assertNull(JsonPointer.parse("/foo/2").find(doc), "index past the end");
        assertNull(JsonPointer.parse("/foo/01").find(doc), "leading zero is not an index");
        assertNull(JsonPointer.parse("/foo/-").find(doc), "'-' does not resolve for reads");
        assertNull(JsonPointer.parse("/missing/deeper").find(doc));
        assertThrows(DataException.class, () -> JsonPointer.parse("foo"), "must start with '/'");
        assertThrows(DataException.class, () -> JsonPointer.parse("/a~2b"), "invalid escape");
        assertThrows(DataException.class, () -> JsonPointer.parse("/a~").get(doc));
        assertThrows(DataException.class, () -> JsonPointer.parse("/missing").get(doc));
    }

    @Test
    void pointerRoundTripsThroughToString() {
        for (String p : new String[]{"", "/", "/a~1b", "/m~0n", "/foo/0"}) {
            assertEquals(p, JsonPointer.parse(p).toString());
        }
    }

    @Test
    void patchAppliesRfc6902ExampleOperations() {
        Value doc = Json.parse("{\"foo\":[\"bar\",\"baz\"]}");
        Value patched = JsonPatch.apply(doc, Json.parse("""
                [
                  {"op":"add","path":"/baz","value":"qux"},
                  {"op":"add","path":"/foo/1","value":"qux"},
                  {"op":"remove","path":"/foo/0"},
                  {"op":"replace","path":"/baz","value":"quux"}
                ]
                """));
        assertEquals("{\"foo\":[\"qux\",\"baz\"],\"baz\":\"quux\"}", Json.write(patched));
        assertEquals("{\"foo\":[\"bar\",\"baz\"]}", Json.write(doc), "input is never modified");
    }

    @Test
    void patchMoveCopyAndTestOperations() {
        Value doc = Json.parse("{\"foo\":{\"bar\":\"baz\",\"waldo\":\"fred\"},\"qux\":{\"corge\":\"grault\"}}");
        Value moved = JsonPatch.apply(doc, Json.parse("[{\"op\":\"move\",\"from\":\"/foo/waldo\",\"path\":\"/qux/thud\"}]"));
        assertEquals("{\"foo\":{\"bar\":\"baz\"},\"qux\":{\"corge\":\"grault\",\"thud\":\"fred\"}}", Json.write(moved));
        Value copied = JsonPatch.apply(doc, Json.parse("[{\"op\":\"copy\",\"from\":\"/foo/bar\",\"path\":\"/copy\"}]"));
        assertEquals("baz", copied.asObject().getString("copy"));
        assertDoesNotThrow(() -> JsonPatch.apply(doc, Json.parse("[{\"op\":\"test\",\"path\":\"/foo/bar\",\"value\":\"baz\"}]")));
        assertThrows(DataException.class, () -> JsonPatch.apply(doc, Json.parse("[{\"op\":\"test\",\"path\":\"/foo/bar\",\"value\":\"x\"}]")));
    }

    @Test
    void patchFailureLeavesNoPartialResult() {
        Value doc = Json.parse("{\"a\":1}");
        String bad = "[{\"op\":\"add\",\"path\":\"/b\",\"value\":2},{\"op\":\"remove\",\"path\":\"/missing\"}]";
        assertThrows(DataException.class, () -> JsonPatch.apply(doc, Json.parse(bad)));
        assertEquals("{\"a\":1}", Json.write(doc));
    }

    @Test
    void patchAddAppendAndInsertPositions() {
        Value doc = Json.parse("{\"a\":[1,2]}");
        assertEquals("{\"a\":[1,2,3]}", Json.write(JsonPatch.apply(doc, Json.parse("[{\"op\":\"add\",\"path\":\"/a/-\",\"value\":3}]"))));
        assertEquals("{\"a\":[0,1,2]}", Json.write(JsonPatch.apply(doc, Json.parse("[{\"op\":\"add\",\"path\":\"/a/0\",\"value\":0}]"))));
        assertThrows(DataException.class, () -> JsonPatch.apply(doc, Json.parse("[{\"op\":\"add\",\"path\":\"/a/5\",\"value\":0}]")));
        assertThrows(DataException.class, () -> JsonPatch.apply(doc, Json.parse("[{\"op\":\"move\",\"from\":\"/a\",\"path\":\"/a/0\"}]")));
    }

    @Test
    void mergePatchFollowsRfc7386Examples() {
        assertEquals("{\"a\":\"c\"}", Json.write(JsonPatch.merge(Json.parse("{\"a\":\"b\"}"), Json.parse("{\"a\":\"c\"}"))));
        assertEquals("{}", Json.write(JsonPatch.merge(Json.parse("{\"a\":\"b\"}"), Json.parse("{\"a\":null}"))));
        assertEquals("{\"a\":\"c\"}", Json.write(JsonPatch.merge(Json.parse("{\"a\":[\"b\"]}"), Json.parse("{\"a\":\"c\"}"))));
        assertEquals("{\"a\":{\"b\":\"c\"}}", Json.write(JsonPatch.merge(Json.parse("{\"a\":\"b\"}"), Json.parse("{\"a\":{\"b\":\"c\"}}"))));
        assertEquals("{\"a\":[1]}", Json.write(JsonPatch.merge(Json.parse("{\"a\":[{\"b\":\"c\"}]}"), Json.parse("{\"a\":[1]}"))));
        assertEquals("[\"c\",\"d\"]", Json.write(JsonPatch.merge(Json.parse("[\"a\",\"b\"]"), Json.parse("[\"c\",\"d\"]"))));
        assertEquals("[\"c\"]", Json.write(JsonPatch.merge(Json.parse("{\"a\":\"b\"}"), Json.parse("[\"c\"]"))));
        assertEquals("null", Json.write(JsonPatch.merge(Json.parse("{\"a\":\"foo\"}"), Json.parse("null"))));
        assertEquals("\"bar\"", Json.write(JsonPatch.merge(Json.parse("{\"a\":\"foo\"}"), Json.parse("\"bar\""))));
        assertEquals("{\"e\":null,\"a\":1}", Json.write(JsonPatch.merge(Json.parse("{\"e\":null}"), Json.parse("{\"a\":1}"))));
        assertEquals("{\"a\":\"b\"}", Json.write(JsonPatch.merge(Json.parse("[1,2]"), Json.parse("{\"a\":\"b\",\"c\":null}"))));
        assertEquals("{\"a\":{\"bb\":{}}}", Json.write(JsonPatch.merge(Json.parse("{}"), Json.parse("{\"a\":{\"bb\":{\"ccc\":null}}}"))));
    }

    private static final String STORE = """
            {"store":{"book":[
              {"category":"reference","author":"Nigel","title":"Sayings","price":8.95},
              {"category":"fiction","author":"Evelyn","title":"Sword","price":12.99,"isbn":"0-553"},
              {"category":"fiction","author":"Herman","title":"Moby","price":22.99,"tags":["a"]}
            ],"bicycle":{"color":"red","price":19.95}}}
            """;

    @Test
    void jsonPathBasicsAndIndexes() {
        Value doc = Json.parse(STORE);
        assertEquals("red", JsonPath.compile("$.store.bicycle.color").evalOne(doc).asString());
        assertEquals("Sayings", JsonPath.compile("$.store.book[0].title").evalOne(doc).asString());
        assertEquals("Moby", JsonPath.compile("$.store.book[-1].title").evalOne(doc).asString());
        assertEquals("Sword", JsonPath.compile("$['store']['book'][1]['title']").evalOne(doc).asString());
        assertNull(JsonPath.compile("$.store.book[9]").evalOne(doc));
    }

    @Test
    void jsonPathWildcardRecursiveUnionAndSlice() {
        Value doc = Json.parse(STORE);
        assertEquals(3, JsonPath.compile("$.store.book[*].title").eval(doc).size());
        assertEquals(4, JsonPath.compile("$..price").eval(doc).size());
        assertEquals(2, JsonPath.compile("$.store.book[0,2].title").eval(doc).size());
        assertEquals(2, JsonPath.compile("$.store.book[0:2].title").eval(doc).size());
        assertEquals(2, JsonPath.compile("$.store.book[:2]").eval(doc).size());
        assertEquals(2, JsonPath.compile("$.store.book[::2]").eval(doc).size());
        assertEquals(3, JsonPath.compile("$.store.book[::-1]").eval(doc).size());
        assertEquals(1, JsonPath.compile("$.store.book[-1:]").eval(doc).size());
    }

    @Test
    void jsonPathFiltersWithComparisonsLogicAndExistence() {
        Value doc = Json.parse(STORE);
        assertEquals(1, JsonPath.compile("$.store.book[?(@.price > 15)]").eval(doc).size());
        assertEquals(1, JsonPath.compile("$.store.book[?(@.title == 'Sword')]").eval(doc).size());
        assertEquals(1, JsonPath.compile("$.store.book[?(@.tags)]").eval(doc).size());
        assertEquals(2, JsonPath.compile("$.store.book[?(@.category == \"fiction\")]").eval(doc).size());
        assertEquals(1, JsonPath.compile("$.store.book[?(@.category == 'fiction' && @.price < 20)]").eval(doc).size());
        assertEquals(2, JsonPath.compile("$.store.book[?(@.price < 10 || @.price > 20)]").eval(doc).size());
        assertEquals(2, JsonPath.compile("$.store.book[?(!(@.category == 'reference'))]").eval(doc).size());
        assertEquals(0, JsonPath.compile("$.store.book[?(@.missing > 1)]").eval(doc).size(), "missing operands never match");
    }

    @Test
    void jsonPathRejectsMalformedQueries() {
        for (String bad : new String[]{"store", "$.", "$[", "$[?(@.x >)]", "$[?(@.x", "$[0", "$['open", "$[0:1:0]", "$.a$"}) {
            assertThrows(DataException.class, () -> JsonPath.compile(bad), bad);
        }
    }

    @Test
    void jsonLinesReadsLazilyAndSkipsBlankLines() throws IOException {
        List<Value> values;
        try (var stream = JsonLines.read(new StringReader("{\"a\":1}\n\n[2]\n\"three\"\n"))) {
            values = stream.collect(Collectors.toList());
        }
        assertEquals(3, values.size());
        assertEquals(1, values.get(0).asObject().getLong("a"));
        assertEquals("three", values.get(2).asString());
    }

    @Test
    void jsonLinesReportsTheFailingLine() {
        DataException e = assertThrows(DataException.class, () -> {
            try (var stream = JsonLines.read(new StringReader("{\"a\":1}\n\n{bad}\n"))) {
                stream.forEach(v -> { });
            }
        });
        assertTrue(e.getMessage().contains("line 3"), e.getMessage());
    }

    @Test
    void jsonLinesWriteRoundTrips() throws IOException {
        StringWriter out = new StringWriter();
        JsonLines.write(List.of(Json.parse("{\"x\":1}"), Json.parse("[true]")), out);
        assertEquals("{\"x\":1}\n[true]\n", out.toString());
        try (var stream = JsonLines.read(new StringReader(out.toString()))) {
            assertEquals(2, stream.count());
        }
    }
}
