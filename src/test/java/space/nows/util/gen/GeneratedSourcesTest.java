package space.nows.util.gen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedSourcesTest {
    @Test void generatedCollectionsMatchTheTemplates() throws IOException {
        Path root = Path.of("src", "main", "java");
        for (Map.Entry<String, String> e : CollectionGenerator.generate().entrySet()) {
            Path file = root.resolve(e.getKey());
            assertTrue(Files.exists(file), "missing generated file " + file);
            String actual = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertEquals(e.getValue(), actual, file + " is stale; run CollectionGenerator");
        }
    }
}
