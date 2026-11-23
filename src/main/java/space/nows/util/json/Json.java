package space.nows.util.json;

import space.nows.util.check.Preconditions;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;
import space.nows.util.error.InvalidUtf8Exception;
import space.nows.util.text.Utf8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Strict JSON parsing and serialization of {@link Value} trees. Input must be UTF-8; output is always UTF-8. */
public final class Json {
    private static final Charset UTF_32BE = Charset.forName("UTF-32BE");
    private static final Charset UTF_32LE = Charset.forName("UTF-32LE");

    private Json() {}

    public static Value parse(String input) {
        return parse(input, ParseOptions.STRICT);
    }

    public static Value parse(String input, ParseOptions options) {
        return new JsonParser(new StringReader(Preconditions.notNull(input, "input")), Preconditions.notNull(options, "options"))
                .parseDocument();
    }

    /** Parses UTF-8 bytes. Use {@link #parseAuto(byte[])} when the encoding is not known to be UTF-8. */
    public static Value parse(byte[] input) {
        return parse(input, ParseOptions.STRICT);
    }

    public static Value parse(byte[] input, ParseOptions options) {
        Preconditions.notNull(input, "input");
        try {
            return parse(Utf8.decode(input, 0, input.length), options);
        } catch (InvalidUtf8Exception e) {
            throw new DataException("malformed UTF-8: " + e.getMessage(), e);
        }
    }

    /** Reads UTF-8 until end of stream. The stream is not closed. */
    public static Value parse(InputStream input) {
        return parse(input, ParseOptions.STRICT);
    }

    public static Value parse(InputStream input, ParseOptions options) {
        Preconditions.notNull(input, "input");
        return parse(new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)), options);
    }

    /** Reads until end of input. The reader is not closed. */
    public static Value parse(Reader input) {
        return parse(input, ParseOptions.STRICT);
    }

    public static Value parse(Reader input, ParseOptions options) {
        return new JsonParser(Preconditions.notNull(input, "input"), Preconditions.notNull(options, "options"))
                .parseDocument();
    }

    /**
     * Parses bytes in UTF-8, UTF-16 or UTF-32, detected from a byte order mark or, failing that, from the zero-byte
     * pattern of the first code units (RFC 8259 section 8.1). Input without either defaults to UTF-8.
     */
    public static Value parseAuto(byte[] input) {
        return parseAuto(input, ParseOptions.STRICT);
    }

    public static Value parseAuto(byte[] input, ParseOptions options) {
        Preconditions.notNull(input, "input");
        Charset charset = detectCharset(input);
        return parse(decodeStrict(input, charset), options);
    }

    /** Like {@link #parseAuto(byte[])}, reading until end of stream. The stream is not closed. */
    public static Value parseAuto(InputStream input) {
        return parseAuto(input, ParseOptions.STRICT);
    }

    public static Value parseAuto(InputStream input, ParseOptions options) {
        Preconditions.notNull(input, "input");
        try {
            byte[] head = input.readNBytes(4);
            Charset charset = detectCharset(head);
            InputStream rest = new SequenceInputStream(new ByteArrayInputStream(head), input);
            return parse(new InputStreamReader(rest, strictDecoder(charset)), options);
        } catch (IOException e) {
            throw new DataException("I/O error while detecting the encoding", e);
        }
    }

    private static Charset detectCharset(byte[] head) {
        int n = head.length;
        if (n >= 4 && head[0] == 0 && head[1] == 0 && head[2] == (byte) 0xFE && head[3] == (byte) 0xFF) return UTF_32BE;
        if (n >= 4 && head[0] == (byte) 0xFF && head[1] == (byte) 0xFE && head[2] == 0 && head[3] == 0) return UTF_32LE;
        if (n >= 3 && head[0] == (byte) 0xEF && head[1] == (byte) 0xBB && head[2] == (byte) 0xBF) return StandardCharsets.UTF_8;
        if (n >= 2 && head[0] == (byte) 0xFE && head[1] == (byte) 0xFF) return StandardCharsets.UTF_16BE;
        if (n >= 2 && head[0] == (byte) 0xFF && head[1] == (byte) 0xFE) return StandardCharsets.UTF_16LE;
        if (n >= 4 && head[0] == 0 && head[1] == 0 && head[2] == 0 && head[3] != 0) return UTF_32BE;
        if (n >= 4 && head[0] != 0 && head[1] == 0 && head[2] == 0 && head[3] == 0) return UTF_32LE;
        if (n >= 2 && head[0] == 0 && head[1] != 0) return StandardCharsets.UTF_16BE;
        if (n >= 2 && head[0] != 0 && head[1] == 0) return StandardCharsets.UTF_16LE;
        return StandardCharsets.UTF_8;
    }

    private static CharsetDecoder strictDecoder(Charset charset) {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
    }

    private static String decodeStrict(byte[] input, Charset charset) {
        try {
            return strictDecoder(charset).decode(ByteBuffer.wrap(input)).toString();
        } catch (CharacterCodingException e) {
            throw new DataException("malformed " + charset.name() + " input", e);
        }
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

    /**
     * Canonical JSON (RFC 8785): sorted members, minimal escapes and ECMAScript number formatting. Two equal documents
     * give the same bytes, which makes the output suitable for hashing and signing.
     */
    public static String canonical(Value value) {
        return Canonical.write(Preconditions.notNull(value, "value"));
    }

    /** Writes compact JSON using the given options. */
    public static String write(Value value, WriteOptions options) {
        return render(value, Preconditions.notNull(options, "options"));
    }

    public static void write(Value value, WriteOptions options, Appendable output) throws IOException {
        new JsonWriter(output, Preconditions.notNull(options, "options")).write(Preconditions.notNull(value, "value"));
    }

    private static String render(Value value, boolean pretty) {
        return render(value, pretty ? WriteOptions.PRETTY : WriteOptions.COMPACT);
    }

    private static String render(Value value, WriteOptions options) {
        StringBuilder output = new StringBuilder();
        try {
            new JsonWriter(output, options).write(Preconditions.notNull(value, "value"));
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
