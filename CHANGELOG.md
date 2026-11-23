# Changelog

## 1.1.0 - 2026-11-23

### JSON
- Added a strict JSON value tree, parser and compact/pretty writers in `space.nows.util.json.Json`.
- Added basic reflection mapping for records, POJOs, arrays, collections, maps, enums and optionals, with property/ignore/include annotations.

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
