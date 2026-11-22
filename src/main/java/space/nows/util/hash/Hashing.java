package space.nows.util.hash;

import space.nows.util.check.Preconditions;
import space.nows.util.io.ByteAccess;

import java.util.zip.CRC32;
import java.util.zip.CRC32C;

/** Non-cryptographic hashes and checksums of byte arrays: xxHash32, xxHash64, CRC-32 and CRC-32C. */
public final class Hashing {
    private Hashing() {}

    private static final int P32_1 = 0x9E3779B1, P32_2 = 0x85EBCA77, P32_3 = 0xC2B2AE3D, P32_4 = 0x27D4EB2F, P32_5 = 0x165667B1;
    private static final long P64_1 = 0x9E3779B185EBCA87L, P64_2 = 0xC2B2AE3D27D4EB4FL, P64_3 = 0x165667B19E3779F9L,
            P64_4 = 0x85EBCA77C2B2AE63L, P64_5 = 0x27D4EB2F165667C5L;

    public static int xxHash32(byte[] data) { return xxHash32(data, 0, Preconditions.notNull(data, "data").length, 0); }

    public static int xxHash32(byte[] data, int off, int len, int seed) {
        Preconditions.notNull(data, "data");
        Preconditions.range(off, len, data.length);
        int p = off;
        int end = off + len;
        int h;
        if (len >= 16) {
            int v1 = seed + P32_1 + P32_2, v2 = seed + P32_2, v3 = seed, v4 = seed - P32_1;
            int limit = end - 16;
            do {
                v1 = round32(v1, ByteAccess.readIntLE(data, p));
                v2 = round32(v2, ByteAccess.readIntLE(data, p + 4));
                v3 = round32(v3, ByteAccess.readIntLE(data, p + 8));
                v4 = round32(v4, ByteAccess.readIntLE(data, p + 12));
                p += 16;
            } while (p <= limit);
            h = Integer.rotateLeft(v1, 1) + Integer.rotateLeft(v2, 7) + Integer.rotateLeft(v3, 12) + Integer.rotateLeft(v4, 18);
        } else {
            h = seed + P32_5;
        }
        h += len;
        while (p + 4 <= end) {
            h = Integer.rotateLeft(h + ByteAccess.readIntLE(data, p) * P32_3, 17) * P32_4;
            p += 4;
        }
        while (p < end) {
            h = Integer.rotateLeft(h + (data[p++] & 0xFF) * P32_5, 11) * P32_1;
        }
        h ^= h >>> 15;
        h *= P32_2;
        h ^= h >>> 13;
        h *= P32_3;
        h ^= h >>> 16;
        return h;
    }

    private static int round32(int acc, int input) { return Integer.rotateLeft(acc + input * P32_2, 13) * P32_1; }

    public static long xxHash64(byte[] data) { return xxHash64(data, 0, Preconditions.notNull(data, "data").length, 0L); }

    public static long xxHash64(byte[] data, int off, int len, long seed) {
        Preconditions.notNull(data, "data");
        Preconditions.range(off, len, data.length);
        int p = off;
        int end = off + len;
        long h;
        if (len >= 32) {
            long v1 = seed + P64_1 + P64_2, v2 = seed + P64_2, v3 = seed, v4 = seed - P64_1;
            int limit = end - 32;
            do {
                v1 = round64(v1, ByteAccess.readLongLE(data, p));
                v2 = round64(v2, ByteAccess.readLongLE(data, p + 8));
                v3 = round64(v3, ByteAccess.readLongLE(data, p + 16));
                v4 = round64(v4, ByteAccess.readLongLE(data, p + 24));
                p += 32;
            } while (p <= limit);
            h = Long.rotateLeft(v1, 1) + Long.rotateLeft(v2, 7) + Long.rotateLeft(v3, 12) + Long.rotateLeft(v4, 18);
            h = mergeRound64(h, v1);
            h = mergeRound64(h, v2);
            h = mergeRound64(h, v3);
            h = mergeRound64(h, v4);
        } else {
            h = seed + P64_5;
        }
        h += len;
        while (p + 8 <= end) {
            h ^= round64(0L, ByteAccess.readLongLE(data, p));
            h = Long.rotateLeft(h, 27) * P64_1 + P64_4;
            p += 8;
        }
        if (p + 4 <= end) {
            h ^= (ByteAccess.readIntLE(data, p) & 0xFFFFFFFFL) * P64_1;
            h = Long.rotateLeft(h, 23) * P64_2 + P64_3;
            p += 4;
        }
        while (p < end) {
            h ^= (data[p++] & 0xFFL) * P64_5;
            h = Long.rotateLeft(h, 11) * P64_1;
        }
        h ^= h >>> 33;
        h *= P64_2;
        h ^= h >>> 29;
        h *= P64_3;
        h ^= h >>> 32;
        return h;
    }

    private static long round64(long acc, long input) { return Long.rotateLeft(acc + input * P64_2, 31) * P64_1; }

    private static long mergeRound64(long acc, long val) { return (acc ^ round64(0L, val)) * P64_1 + P64_4; }

    /** CRC-32 (the zlib/gzip polynomial) as an unsigned 32-bit value. */
    public static long crc32(byte[] data) { return crc32(data, 0, Preconditions.notNull(data, "data").length); }

    public static long crc32(byte[] data, int off, int len) {
        Preconditions.notNull(data, "data");
        Preconditions.range(off, len, data.length);
        CRC32 crc = new CRC32();
        crc.update(data, off, len);
        return crc.getValue();
    }

    /** CRC-32C (Castagnoli, hardware accelerated on most CPUs) as an unsigned 32-bit value. */
    public static long crc32c(byte[] data) { return crc32c(data, 0, Preconditions.notNull(data, "data").length); }

    public static long crc32c(byte[] data, int off, int len) {
        Preconditions.notNull(data, "data");
        Preconditions.range(off, len, data.length);
        CRC32C crc = new CRC32C();
        crc.update(data, off, len);
        return crc.getValue();
    }
}
