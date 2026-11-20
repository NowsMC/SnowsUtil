package space.nows.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stream and file helpers: read, write, copy, close quietly. */
public final class IOUtils {
    private IOUtils() {}

    private static final int DEFAULT_BUFFER = 8192;

    public static byte[] readAll(InputStream in) throws IOException { return in.readAllBytes(); }

    public static byte[] readFile(Path path) throws IOException { return Files.readAllBytes(path); }

    public static void writeFile(Path path, byte[] data) throws IOException { Files.write(path, data); }

    public static long copy(InputStream in, OutputStream out) throws IOException { return copy(in, out, DEFAULT_BUFFER); }

    public static long copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
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
