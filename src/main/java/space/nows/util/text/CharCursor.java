package space.nows.util.text;

import space.nows.util.check.Preconditions;
import space.nows.util.error.DataException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Character reader with one character of lookahead that tracks the number of characters consumed, so parsers
 * can report where a problem occurred. Shared by text-format parsers.
 */
public final class CharCursor {
    public static final int EOF = -1;

    private static final int NONE = Integer.MIN_VALUE;

    private final BufferedReader input;
    private int lookahead = NONE;
    private long offset;
    private long line = 1;
    private long column = 1;

    public CharCursor(Reader input) {
        this.input = input instanceof BufferedReader buffered
                ? buffered : new BufferedReader(Preconditions.notNull(input, "input"));
    }

    /** Returns the next character without consuming it, or {@link #EOF}. */
    public int peek() throws IOException {
        if (lookahead == NONE) lookahead = input.read();
        return lookahead;
    }

    /** Consumes and returns the next character, or {@link #EOF}. */
    public int read() throws IOException {
        int value = peek();
        lookahead = NONE;
        if (value != EOF) {
            offset++;
            if (value == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return value;
    }

    /** Number of characters consumed so far. */
    public long offset() {
        return offset;
    }

    /** One-based line of the next character to be read. */
    public long line() {
        return line;
    }

    /** One-based column of the next character to be read. */
    public long column() {
        return column;
    }

    public DataException error(String message) {
        return new DataException(message + " (at line " + line + ", column " + column + ", char offset " + offset + ")");
    }
}
