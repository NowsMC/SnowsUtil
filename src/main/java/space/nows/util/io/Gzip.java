package space.nows.util.io;

import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.Checks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** In-memory GZIP compression and decompression on top of the JDK's {@code java.util.zip} streams. */
public final class Gzip {
    private Gzip() {}

    /** Cheap check of the two-byte GZIP magic number; it does not validate the rest. */
    public static boolean isGzip(byte[] data) {
        return data != null && data.length >= 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B;
    }

    public static byte[] gzip(byte[] data) throws IOException {
        Checks.notNull(data, "data");
        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(32, data.length / 2));
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(data);
        }
        return bos.toByteArray();
    }

    /** Decompresses without a size limit. Use {@link #gunzip(byte[], int)} for untrusted input. */
    public static byte[] gunzip(byte[] data) throws IOException {
        Checks.notNull(data, "data");
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return gz.readAllBytes();
        }
    }

    /**
     * Decompresses, refusing to produce more than {@code maxBytes} (protection against decompression bombs).
     *
     * @throws CapacityExceededException if the decompressed size would exceed {@code maxBytes}
     */
    public static byte[] gunzip(byte[] data, int maxBytes) throws IOException {
        Checks.notNull(data, "data");
        Checks.nonNegative(maxBytes, "maxBytes");
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return IOUtils.readAll(gz, maxBytes);
        }
    }
}
