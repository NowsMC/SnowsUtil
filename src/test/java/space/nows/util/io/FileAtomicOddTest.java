package space.nows.util.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FileAtomicOddTest {
    @Test
    void readersNeverSeeAPartialWriteWhileWritersReplaceTheFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("state.bin");
        byte[] first = new byte[64 * 1024];
        byte[] second = new byte[64 * 1024];
        Arrays.fill(first, (byte) 1);
        Arrays.fill(second, (byte) 2);
        FileIO.writeFileAtomic(file, first);

        AtomicBoolean done = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < 300; i++) FileIO.writeFileAtomic(file, i % 2 == 0 ? second : first);
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                done.set(true);
            }
        });
        writer.start();
        List<String> bad = new ArrayList<>();
        while (!done.get()) {
            byte[] seen = Files.readAllBytes(file);
            byte firstByte = seen[0];
            for (byte b : seen) if (b != firstByte) { bad.add("mixed contents"); break; }
            if (seen.length != first.length) bad.add("length " + seen.length);
        }
        writer.join();
        assertNull(failure.get(), "writer failed: " + failure.get());
        assertTrue(bad.isEmpty(), "torn reads: " + bad);
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count(), "no temporary files may remain");
        }
    }

    @Test
    void writingToMissingDirectoryFailsWithoutCreatingTheFile(@TempDir Path dir) {
        Path file = dir.resolve("missing").resolve("x.bin");
        assertThrows(Exception.class, () -> FileIO.writeFileAtomic(file, new byte[]{1}));
        assertFalse(Files.exists(file));
    }
}
