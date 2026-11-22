package space.nows.util.io;

import space.nows.util.check.Preconditions;
import space.nows.util.error.CapacityExceededException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Reading and copying {@link InputStream}s with explicit size guards. */
public final class Streams {
    private Streams() {}

    private static final int DEFAULT_BUFFER = 8192;

    public static byte[] readAll(InputStream in) throws IOException { return Preconditions.notNull(in, "in").readAllBytes(); }

    /** Reads the whole stream, but fails with {@link CapacityExceededException} instead of buffering more than {@code maxBytes}. */
    public static byte[] readAll(InputStream in, int maxBytes) throws IOException {
        Preconditions.notNull(in, "in");
        Preconditions.nonNegative(maxBytes, "maxBytes");
        byte[] data = in.readNBytes(maxBytes);
        if (data.length == maxBytes && in.read() != -1) {
            throw new CapacityExceededException("stream", (long) maxBytes + 1, maxBytes);
        }
        return data;
    }

    /** Reads exactly {@code n} bytes or throws {@link EOFException}. */
    public static byte[] readExactly(InputStream in, int n) throws IOException {
        Preconditions.notNull(in, "in");
        Preconditions.nonNegative(n, "n");
        byte[] data = in.readNBytes(n);
        if (data.length < n) throw new EOFException("expected " + n + " bytes but stream ended after " + data.length);
        return data;
    }

    public static long copy(InputStream in, OutputStream out) throws IOException { return copy(in, out, DEFAULT_BUFFER); }

    /** Copies until EOF; {@code bufferSize} must be positive (a zero-length buffer would spin forever). */
    public static long copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
        Preconditions.notNull(in, "in");
        Preconditions.notNull(out, "out");
        Preconditions.positive(bufferSize, "bufferSize");
        byte[] buf = new byte[bufferSize];
        long total = 0;
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
            total += n;
        }
        return total;
    }

    public static void closeQuietly(AutoCloseable c) {
        if (c == null) return;
        try { c.close(); } catch (Exception ignored) { /* best effort */ }
    }
}
