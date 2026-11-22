# SnowsUtil

Low-allocation Java utility library — package root `space.nows.util`.

## Generated sources

The hash maps, hash sets and primitive lists in `space.nows.util.collections` and the typed helpers in `space.nows.util.array` (`ArrayCapacity`, `ArrayEdit`, `ArrayOrder`, `ArrayReorder`, `ArraySearch`) are generated from `src/test/resources/templates`. After editing a template, run `mvn test-compile` and then `java -cp target/test-classes space.nows.util.gen.SourceGenerator`; `GeneratedSourcesTest` fails while the two disagree.
