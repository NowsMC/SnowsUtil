package space.nows.util.json;

import space.nows.util.check.Preconditions;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * JSON Lines (NDJSON): one JSON value per line. Blank lines are skipped. Reading is lazy, so a large file is never
 * held in memory at once.
 */
public final class JsonLines {
    private JsonLines() {}

    /**
     * Returns a lazily parsed stream of the values in {@code reader}. The stream must be closed to release the reader;
     * a malformed line fails with a {@link DataException} that names its line number.
     */
    public static Stream<Value> read(Reader reader) {
        return read(reader, ParseOptions.STRICT);
    }

    public static Stream<Value> read(Reader reader, ParseOptions options) {
        Preconditions.notNull(options, "options");
        BufferedReader lines = reader instanceof BufferedReader buffered
                ? buffered : new BufferedReader(Preconditions.notNull(reader, "reader"));
        Iterator<Value> values = new LineIterator(lines, options);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(values, Spliterator.ORDERED | Spliterator.NONNULL), false)
                .onClose(() -> closeQuietly(lines));
    }

    /** Writes each value on its own line, each terminated by {@code \n}. The writer is flushed but not closed. */
    public static void write(Iterable<? extends Value> values, Writer out) throws IOException {
        Preconditions.notNull(values, "values");
        Preconditions.notNull(out, "out");
        for (Value value : values) {
            Json.write(value, out);
            out.write('\n');
        }
        out.flush();
    }

    private static void closeQuietly(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException ignored) {
            // the stream is already finished; nothing useful to report
        }
    }

    private static final class LineIterator implements Iterator<Value> {
        private final BufferedReader lines;
        private final ParseOptions options;
        private long lineNumber;
        private Value next;
        private boolean done;

        LineIterator(BufferedReader lines, ParseOptions options) {
            this.lines = lines;
            this.options = options;
        }

        @Override public boolean hasNext() {
            if (next == null && !done) next = advance();
            return next != null;
        }

        @Override public Value next() {
            if (!hasNext()) throw new NoSuchElementException();
            Value value = next;
            next = null;
            return value;
        }

        private Value advance() {
            try {
                String line;
                while ((line = lines.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) continue;
                    try {
                        return Json.parse(line, options);
                    } catch (DataException e) {
                        throw new DataException("JSON Lines line " + lineNumber + ": " + e.getMessage(), e);
                    }
                }
                done = true;
                return null;
            } catch (IOException e) {
                throw new DataException("I/O error reading JSON Lines at line " + (lineNumber + 1), e);
            }
        }
    }
}
