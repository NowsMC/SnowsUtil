package space.nows.util.hash;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HashingTest {
    private static byte[] ascii(String s) { return s.getBytes(StandardCharsets.US_ASCII); }

    @Test void xxHash32KnownVectors() {
        assertEquals(0x02CC5D05, Hashing.xxHash32(new byte[0]));
        assertEquals(0x32D153FF, Hashing.xxHash32(ascii("abc")));
        assertEquals(0xE2293B2F, Hashing.xxHash32(ascii("Nobody inspects the spammish repetition")));
    }

    @Test void xxHash64KnownVectors() {
        assertEquals(0xEF46DB3751D8E999L, Hashing.xxHash64(new byte[0]));
        assertEquals(0x44BC2CF5AD770999L, Hashing.xxHash64(ascii("abc")));
        assertEquals(0xFBCEA83C8A378BF1L, Hashing.xxHash64(ascii("Nobody inspects the spammish repetition")));
    }

    @Test void rangeHashingMatchesCopiedSlice() {
        byte[] data = new byte[200];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 31 + 7);
        for (int len : new int[] {0, 1, 3, 4, 7, 8, 15, 16, 17, 31, 32, 33, 63, 64, 100}) {
            byte[] slice = java.util.Arrays.copyOfRange(data, 13, 13 + len);
            assertEquals(Hashing.xxHash32(slice, 0, len, 5), Hashing.xxHash32(data, 13, len, 5));
            assertEquals(Hashing.xxHash64(slice, 0, len, 5L), Hashing.xxHash64(data, 13, len, 5L));
        }
        assertNotEquals(Hashing.xxHash64(data, 0, 100, 1L), Hashing.xxHash64(data, 0, 100, 2L));
    }

    @Test void checksums() {
        assertEquals(0xCBF43926L, Hashing.crc32(ascii("123456789")));
        assertEquals(0xE3069283L, Hashing.crc32c(ascii("123456789")));
        assertEquals(0L, Hashing.crc32(new byte[0]));
    }

    @Test void argumentChecks() {
        assertThrows(NullPointerException.class, () -> Hashing.xxHash64(null));
        assertThrows(IndexOutOfBoundsException.class, () -> Hashing.xxHash32(new byte[4], 2, 3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Hashing.crc32c(new byte[4], -1, 2));
    }
}
