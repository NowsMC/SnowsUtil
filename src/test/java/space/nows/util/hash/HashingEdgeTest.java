package space.nows.util.hash;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Test;
import space.nows.util.bits.BitRanges;
import space.nows.util.bits.BitSearch;
import space.nows.util.concurrent.MpmcQueue;
import space.nows.util.concurrent.ObjectPool;
import space.nows.util.concurrent.SpscQueue;
import space.nows.util.hash.Hashing;
import space.nows.util.net.HostPort;
import static org.junit.jupiter.api.Assertions.*;

class HashingEdgeTest {
    @Test
    void checksumsMatchJdk() {
        Random r = new Random(22);
        for (int trial = 0; trial < 500; trial++) {
            byte[] data = new byte[r.nextInt(300)];
            r.nextBytes(data);
            CRC32 c = new CRC32();
            c.update(data);
            assertEquals(c.getValue(), Hashing.crc32(data));
            CRC32C cc = new CRC32C();
            cc.update(data);
            assertEquals(cc.getValue(), Hashing.crc32c(data));
        }
    }

    @Test
    void xxHashKnownVectors() {
        byte[] empty = new byte[0];
        byte[] abc = "abc".getBytes(StandardCharsets.US_ASCII);
        assertEquals(0x02CC5D05, Hashing.xxHash32(empty));
        assertEquals(0x32D153FF, Hashing.xxHash32(abc));
        assertEquals(0xEF46DB3751D8E999L, Hashing.xxHash64(empty));
        assertEquals(0x44BC2CF5AD770999L, Hashing.xxHash64(abc));
    }

    @Test
    void hashOverSubrangeEqualsHashOfCopy() {
        Random r = new Random(23);
        for (int trial = 0; trial < 300; trial++) {
            byte[] data = new byte[40];
            r.nextBytes(data);
            int off = r.nextInt(20);
            int len = r.nextInt(20);
            byte[] copy = java.util.Arrays.copyOfRange(data, off, off + len);
            assertEquals(Hashing.xxHash32(copy), Hashing.xxHash32(data, off, len, 0));
            assertEquals(Hashing.xxHash64(copy), Hashing.xxHash64(data, off, len, 0L));
            assertEquals(Hashing.crc32(copy), Hashing.crc32(data, off, len));
        }
    }

}
