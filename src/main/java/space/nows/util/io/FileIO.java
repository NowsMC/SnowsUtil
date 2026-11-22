package space.nows.util.io;

import space.nows.util.check.Preconditions;
import space.nows.util.error.CapacityExceededException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Whole-file and ranged reads and writes as byte arrays, plus read-only memory mapping. */
public final class FileIO {
    private FileIO() {}

    public static byte[] readFile(Path path) throws IOException { return Files.readAllBytes(Preconditions.notNull(path, "path")); }

    /** Reads the whole file, failing with {@link CapacityExceededException} instead of buffering more than {@code maxBytes}. */
    public static byte[] readFile(Path path, int maxBytes) throws IOException {
        Preconditions.notNull(path, "path");
        Preconditions.nonNegative(maxBytes, "maxBytes");
        try (InputStream in = Files.newInputStream(path)) {
            return Streams.readAll(in, maxBytes);
        }
    }

    /** Reads exactly {@code len} bytes starting at {@code offset}, or throws {@link EOFException} if the file is shorter. */
    public static byte[] readRange(Path path, long offset, int len) throws IOException {
        Preconditions.notNull(path, "path");
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0 but was " + offset);
        Preconditions.nonNegative(len, "len");
        byte[] out = new byte[len];
        ByteBuffer buf = ByteBuffer.wrap(out);
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            long pos = offset;
            while (buf.hasRemaining()) {
                int n = ch.read(buf, pos);
                if (n < 0) throw new EOFException("expected " + len + " bytes at offset " + offset + " but file ended after " + buf.position());
                pos += n;
            }
        }
        return out;
    }

    /**
     * Maps the whole file read-only. The mapping stays valid after this call returns, but the memory is only
     * released when the buffer is garbage collected, and the file must not be truncated while it is in use.
     */
    public static MappedByteBuffer mapReadOnly(Path path) throws IOException {
        Preconditions.notNull(path, "path");
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            long size = ch.size();
            if (size > Integer.MAX_VALUE) throw new CapacityExceededException("mapped file", size, Integer.MAX_VALUE);
            return ch.map(FileChannel.MapMode.READ_ONLY, 0, size);
        }
    }

    public static void writeFile(Path path, byte[] data) throws IOException {
        Files.write(Preconditions.notNull(path, "path"), Preconditions.notNull(data, "data"));
    }

    /**
     * Writes to a temporary file in the same directory and moves it over {@code path}, so readers never see a
     * half-written file. The move is atomic where the file system supports it.
     */
    public static void writeFileAtomic(Path path, byte[] data) throws IOException {
        Preconditions.notNull(path, "path");
        Preconditions.notNull(data, "data");
        Path target = path.toAbsolutePath();
        Path tmp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.write(tmp, data);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
