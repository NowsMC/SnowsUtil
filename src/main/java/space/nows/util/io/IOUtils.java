package space.nows.util.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    public static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(32, data.length / 2));
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(data);
        }
        return bos.toByteArray();
    }

    public static byte[] gunzip(byte[] data) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return gz.readAllBytes();
        }
    }

    public static void closeQuietly(AutoCloseable c) {
        if (c == null) return;
        try { c.close(); } catch (Exception ignored) {   }
    }
}
