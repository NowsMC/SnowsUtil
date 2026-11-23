# Changelog

## 1.1.0 - 2026-11-23

### Data and JSON
- Added the format-neutral value tree `space.nows.util.data.Value`, with `Obj`, `Arr`, `Str`, `Num`, `Bool`, `Null` and `Kind`.
- Added `space.nows.util.data.mapping.Mapper` for converting between Java objects and value trees by reflection, with `Property`, `Ignore` and `Include` annotations. It supports records, classes including inherited fields, arrays, collections, maps, enums and optionals.
- Added `space.nows.util.json.Json` for strict JSON parsing and compact or pretty writing.
- `Mapper` rejects JSON `null` for primitive properties, duplicate property names (including fields shadowed by a subclass), and numbers that overflow `float` or `double`, instead of substituting a value.
- `Mapper` reads map keys of numeric, boolean and enum types back from text, and writes enum keys by their property names.
- JSON `-0` and `-0.0` keep their sign when read as `double` or `float`, and are written back as `-0`. The parser skips a leading byte order mark.

### Text
- Added `Utf8`: strict UTF-8 decoding that reports the offset of the invalid byte, and encoding without temporary arrays.
- Added `CharCursor` for character-level parsers that report error offsets.
- `Hex` gained `digitValue` and `lowerDigit`.
- `NumberParser` gained `tryParseInt` and `tryParseLong`, which report malformed or out-of-range text as an empty result.

### I/O
- `ByteBuf.writeUtf8` and `writeString` no longer allocate a temporary byte array. The output bytes are unchanged.

### Errors
- Added `DataException` and `InvalidUtf8Exception`.

### Changed
- `Rounding.floor`, `ceil` and `round` throw `IllegalArgumentException` for NaN instead of returning 0. Code that relied on the 0 must check for NaN first.

### Fixed
- `ObjectPool` keeps at most `maxIdle` released objects. `maxIdle()` returns the configured value rather than the queue's rounded capacity, and a pool with `maxIdle` of 0 keeps nothing.

## 1.0.0 - 2026-11-22

First release. Requires Java 17 and has no runtime dependencies.

### Collections
- Open-addressing hash maps for primitive keys and values with no boxing: `IntIntMap`, `IntLongMap`, `IntObjectMap`, `LongIntMap`, `LongLongMap`, `LongObjectMap`, `ObjectIntMap`, `ObjectLongMap`. All offer `put`, `get`, `getOrDefault`, `putIfAbsent`, `remove`, `forEach`, `trim`, `copy`, `toJavaMap`, with `addTo` and `merge` for numeric values and `computeIfAbsent` for object values.
- `IntHashSet` and `LongHashSet` with `addAll`, `removeAll` and `retainAll`.
- `IntList`, `LongList`, `IntArrayDeque` and `IntMinHeap`.
- `SmallArrayMap`, and the array-indexed `EnumArrayMap` and `IntRangeArrayMap`.
- The hash maps, sets and lists are generated from templates; see the README.

### Arrays and bits
- Typed array helpers for `byte[]`, `int[]`, `long[]` and object arrays: `ArrayEdit`, `ArraySearch` (eight bytes at a time for `byte[]`), `ArrayOrder`, `ArrayReorder` (including Fisher-Yates shuffle with any `RandomGenerator`) and `ArrayCapacity`.
- `ArraySort` radix sorts for `int[]` and `long[]`, and `ArrayMath` sums and prefix sums.
- `BitArrays`, `BitRanges`, `BitSearch`, `BitLogic`, `BitField` and `IntBits`.

### I/O and hashing
- `ByteBuf` with independent read and write positions, big and little endian access, varints, zigzag, length-prefixed strings and `ByteBuffer` bridges, including direct buffers.
- `ByteAccess`, `Streams`, `Gzip` and `FileIO` (bounded reads, ranged reads, memory mapping, atomic writes).
- `Hashing` with xxHash32, xxHash64, CRC-32 and CRC-32C.

### Concurrency
- `Parallel` loops, sums and reductions, `SpscQueue`, `MpmcQueue`, `ObjectPool`, `Backoff` and `NamedThreadFactory`.

### Math
- Scalar helpers: `Angles`, `Bounds`, `Interpolation`, `Modulo`, `PowersOfTwo`, `Rounding`, `Signs` and table-based `FastTrig` with interpolated variants.
- `geometry`: distances, containment, overlap, segment intersection and polygons.
- `linear`: `Vec2`, `Vec3`, `Vec4`, `Mat4`, `Mat4Projection`, `Mat4Inverse` and `Quaternion`.

### Random
- `FastRandom`, a seedable xoshiro256** generator.
- `TrueRandom`, non-deterministic numbers from the operating system's entropy source through `SecureRandom`, with helpers for bytes, hex tokens, UUIDs and random strings.

### Networking
- `HostPort` parsing and formatting, `Ports` probing, and `TcpProxy`, a small TCP forwarding proxy with connection limits, idle timeouts and byte counters.

### Text and utilities
- `Hex`, `NumberParser`, `Strings`, `Padding` and `StringSplit`.
- `Preconditions`, the exception types in `error`, and the primitive consumer interfaces in `function`.
