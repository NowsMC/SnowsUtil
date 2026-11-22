package space.nows.util.io;

import space.nows.util.check.Preconditions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Whole-file reads and writes as byte arrays. */
public final class FileIO {
    private FileIO() {}

    public static byte[] readFile(Path path) throws IOException { return Files.readAllBytes(Preconditions.notNull(path, "path")); }

    public static void writeFile(Path path, byte[] data) throws IOException {
        Files.write(Preconditions.notNull(path, "path"), Preconditions.notNull(data, "data"));
    }
}
