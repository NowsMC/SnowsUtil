package space.nows.util.io;

import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.Checks;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stream and file helpers built on the JDK's {@code InputStream}/{@code Files} APIs, with explicit size guards. */
public final class IOUtils {
    private IOUtils() {}

    private static final int DEFAULT_BUFFER = 8192;

    public static byte[] readAll(InputStream in) throws IOException { return Checks.notNull(in, "in").readAllBytes(); }

    /** Reads the whole stream, but fails with {@link CapacityExceededException} instead of buffering more than {@code maxBytes}. */
    public static byte[] readAll(InputStream in, int maxBytes) throws IOException {
        Checks.notNull(in, "in");
        Checks.nonNegative(maxBytes, "maxBytes");
        byte[] data = in.readNBytes(maxBytes);
        if (data.length == maxBytes && in.read() != -1) {
            throw new CapacityExceededException("stream", (long) maxBytes + 1, maxBytes);
        }
        return data;
    }

    /** Reads exactly {@code n} bytes or throws {@link EOFException}. */
    public static byte[] readExactly(InputStream in, int n) throws IOException {
        Checks.notNull(in, "in");
        Checks.nonNegative(n, "n");
        byte[] data = in.readNBytes(n);
        if (data.length < n) throw new EOFException("expected " + n + " bytes but stream ended after " + data.length);
        return data;
    }

    public static byte[] readFile(Path path) throws IOException { return Files.readAllBytes(Checks.notNull(path, "path")); }

    public static void writeFile(Path path, byte[] data) throws IOException {
        Files.write(Checks.notNull(path, "path"), Checks.notNull(data, "data"));
    }

    public static long copy(InputStream in, OutputStream out) throws IOException { return copy(in, out, DEFAULT_BUFFER); }

    /** Copies until EOF; {@code bufferSize} must be positive (a zero-length buffer would spin forever). */
    public static long copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
        Checks.notNull(in, "in");
        Checks.notNull(out, "out");
        Checks.positive(bufferSize, "bufferSize");
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
        try { c.close(); } catch (Exception ignored) {   }
    }
}
