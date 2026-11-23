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
        if (value != EOF) offset++;
        return value;
    }

    /** Number of characters consumed so far. */
    public long offset() {
        return offset;
    }

    public DataException error(String message) {
        return new DataException(message + " (at char offset " + offset + ")");
    }
}
