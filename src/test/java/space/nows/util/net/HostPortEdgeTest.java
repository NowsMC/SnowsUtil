package space.nows.util.net;

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

class HostPortEdgeTest {
    @Test
    void hostPortRejectsOutOfRangeAndEmptyHosts() {
        assertThrows(IllegalArgumentException.class, () -> HostPort.parse("example.com:70000"), "port above 65535");
        assertThrows(IllegalArgumentException.class, () -> HostPort.parse("example.com:99999999999"), "port overflows int");
        assertThrows(IllegalArgumentException.class, () -> HostPort.parse(":8080"), "empty host");
        assertEquals(new HostPort("[::1]".substring(1, 4), 80), HostPort.parse("[::1]:80"));
        assertEquals("[::1]:80", HostPort.parse("[::1]:80").toString());
        assertEquals(0, HostPort.parse("host:0").port());
    }

}
