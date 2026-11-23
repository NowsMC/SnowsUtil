package space.nows.util.json;

import space.nows.util.check.Preconditions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Strict JSON parsing and serialization. Input must be UTF-8; output is always UTF-8. */
public final class Json {
    /** Maximum nesting depth of arrays and objects, enforced when parsing and writing. */
    public static final int MAX_DEPTH = 512;

    private Json() {}

    public static JsonValue parse(String input) {
        return new JsonParser(new StringReader(Preconditions.notNull(input, "input"))).parseDocument();
    }

    public static JsonValue parse(byte[] input) {
        return parse(new ByteArrayInputStream(Preconditions.notNull(input, "input")));
    }

    /** Reads until end of stream. The stream is not closed. */
    public static JsonValue parse(InputStream input) {
        Preconditions.notNull(input, "input");
        return parse(new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)));
    }

    /** Reads until end of input. The reader is not closed. */
    public static JsonValue parse(Reader input) {
        return new JsonParser(Preconditions.notNull(input, "input")).parseDocument();
    }

    public static String write(JsonValue value) { return render(value, false); }

    public static String writePretty(JsonValue value) { return render(value, true); }

    public static void write(JsonValue value, Appendable output) throws IOException {
        new JsonWriter(output, false).write(Preconditions.notNull(value, "value"));
    }

    public static void writePretty(JsonValue value, Appendable output) throws IOException {
        new JsonWriter(output, true).write(Preconditions.notNull(value, "value"));
    }

    /** Writes UTF-8 bytes. The stream is flushed but not closed. */
    public static void write(JsonValue value, OutputStream output) throws IOException {
        writeEncoded(value, output, false);
    }

    /** Writes indented UTF-8 bytes. The stream is flushed but not closed. */
    public static void writePretty(JsonValue value, OutputStream output) throws IOException {
        writeEncoded(value, output, true);
    }

    private static String render(JsonValue value, boolean pretty) {
        StringBuilder output = new StringBuilder();
        try {
            new JsonWriter(output, pretty).write(Preconditions.notNull(value, "value"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return output.toString();
    }

    private static void writeEncoded(JsonValue value, OutputStream output, boolean pretty) throws IOException {
        Preconditions.notNull(output, "output");
        Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        new JsonWriter(writer, pretty).write(Preconditions.notNull(value, "value"));
        writer.flush();
    }
}
