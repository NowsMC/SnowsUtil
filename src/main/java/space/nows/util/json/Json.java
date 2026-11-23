package space.nows.util.json;

import space.nows.util.check.Preconditions;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;
import space.nows.util.error.InvalidUtf8Exception;
import space.nows.util.text.Utf8;

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

/** Strict JSON parsing and serialization of {@link Value} trees. Input must be UTF-8; output is always UTF-8. */
public final class Json {
    private Json() {}

    public static Value parse(String input) {
        return new JsonParser(new StringReader(Preconditions.notNull(input, "input"))).parseDocument();
    }

    public static Value parse(byte[] input) {
        Preconditions.notNull(input, "input");
        try {
            return parse(Utf8.decode(input, 0, input.length));
        } catch (InvalidUtf8Exception e) {
            throw new DataException("malformed UTF-8: " + e.getMessage(), e);
        }
    }

    /** Reads until end of stream. The stream is not closed. */
    public static Value parse(InputStream input) {
        Preconditions.notNull(input, "input");
        return parse(new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)));
    }

    /** Reads until end of input. The reader is not closed. */
    public static Value parse(Reader input) {
        return new JsonParser(Preconditions.notNull(input, "input")).parseDocument();
    }

    public static String write(Value value) { return render(value, false); }

    public static String writePretty(Value value) { return render(value, true); }

    public static void write(Value value, Appendable output) throws IOException {
        new JsonWriter(output, false).write(Preconditions.notNull(value, "value"));
    }

    public static void writePretty(Value value, Appendable output) throws IOException {
        new JsonWriter(output, true).write(Preconditions.notNull(value, "value"));
    }

    /** Writes UTF-8 bytes. The stream is flushed but not closed. */
    public static void write(Value value, OutputStream output) throws IOException {
        writeEncoded(value, output, false);
    }

    /** Writes indented UTF-8 bytes. The stream is flushed but not closed. */
    public static void writePretty(Value value, OutputStream output) throws IOException {
        writeEncoded(value, output, true);
    }

    private static String render(Value value, boolean pretty) {
        StringBuilder output = new StringBuilder();
        try {
            new JsonWriter(output, pretty).write(Preconditions.notNull(value, "value"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return output.toString();
    }

    private static void writeEncoded(Value value, OutputStream output, boolean pretty) throws IOException {
        Preconditions.notNull(output, "output");
        Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        new JsonWriter(writer, pretty).write(Preconditions.notNull(value, "value"));
        writer.flush();
    }
}
