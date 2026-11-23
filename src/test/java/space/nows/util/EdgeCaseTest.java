package space.nows.util;

import org.junit.jupiter.api.Test;
import space.nows.util.bits.BitRanges;
import space.nows.util.bits.BitSearch;
import space.nows.util.concurrent.MpmcQueue;
import space.nows.util.concurrent.ObjectPool;
import space.nows.util.concurrent.SpscQueue;
import space.nows.util.hash.Hashing;
import space.nows.util.net.HostPort;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;

import static org.junit.jupiter.api.Assertions.*;

class EdgeCaseTest {
    @Test
    void bitRangesAndSearchMatchBitSet() {
        Random r = new Random(21);
        for (int trial = 0; trial < 3000; trial++) {
            int words = 1 + r.nextInt(4);
            long[] bits = new long[words];
            BitSet ref = new BitSet();
            int cap = words * 64;
            for (int op = 0; op < 20; op++) {
                int from = r.nextInt(cap + 1);
                int to = from + r.nextInt(cap - from + 1);
                if (r.nextBoolean()) {
                    BitRanges.setRange(bits, from, to);
                    ref.set(from, to);
                } else {
                    BitRanges.clearRange(bits, from, to);
                    ref.clear(from, to);
                }
                assertEquals(ref.cardinality(), java.util.Arrays.stream(bits).map(Long::bitCount).sum(), "cardinality");
                int probe = r.nextInt(cap);
                assertEquals(ref.nextSetBit(probe), BitSearch.nextSetBit(bits, probe), "nextSetBit " + probe);
                assertEquals(ref.previousSetBit(probe), BitSearch.previousSetBit(bits, probe), "previousSetBit " + probe);
                int nbits = 1 + r.nextInt(cap);
                int expected = ref.nextClearBit(probe);
                if (expected >= nbits || probe >= nbits) expected = -1;
                assertEquals(expected, BitSearch.nextClearBit(bits, probe, nbits), "nextClearBit " + probe + " nbits " + nbits);
            }
        }
    }

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

    @Test
    void spscQueueHonoursCapacityAndFifo() {
        SpscQueue<Integer> q = new SpscQueue<>(3);
        assertEquals(4, q.capacity());
        for (int i = 0; i < 4; i++) assertTrue(q.offer(i));
        assertFalse(q.offer(99), "queue should be full at capacity");
        assertEquals(0, q.remainingCapacity());
        for (int i = 0; i < 4; i++) assertEquals(Integer.valueOf(i), q.poll());
        assertNull(q.poll());
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());
    }

    @Test
    void mpmcQueueHonoursCapacityAndFifo() {
        MpmcQueue<Integer> q = new MpmcQueue<>(5);
        assertEquals(8, q.capacity());
        for (int i = 0; i < 8; i++) assertTrue(q.offer(i));
        assertFalse(q.offer(99));
        for (int i = 0; i < 8; i++) assertEquals(Integer.valueOf(i), q.poll());
        assertNull(q.poll());
    }

    @Test
    void mpmcQueueStressKeepsEveryElement() throws Exception {
        MpmcQueue<Integer> q = new MpmcQueue<>(64);
        int producers = 4, perProducer = 50_000;
        AtomicInteger consumed = new AtomicInteger();
        long[] sums = new long[1];
        Thread[] threads = new Thread[producers + 2];
        for (int p = 0; p < producers; p++) {
            int base = p * perProducer;
            threads[p] = new Thread(() -> {
                for (int i = 0; i < perProducer; i++) {
                    while (!q.offer(base + i)) Thread.onSpinWait();
                }
            });
        }
        int total = producers * perProducer;
        for (int c = 0; c < 2; c++) {
            threads[producers + c] = new Thread(() -> {
                while (consumed.get() < total) {
                    Integer v = q.poll();
                    if (v == null) { Thread.onSpinWait(); continue; }
                    synchronized (sums) {
                        sums[0] += v;
                    }
                    consumed.incrementAndGet();
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join(30_000);
        assertEquals(total, consumed.get());
        long expected = 0;
        for (int v = 0; v < total; v++) expected += v;
        assertEquals(expected, sums[0]);
        assertNull(q.poll());
    }

    @Test
    void objectPoolResetsAndBoundsIdleObjects() {
        List<Object> created = new ArrayList<>();
        AtomicInteger resets = new AtomicInteger();
        ObjectPool<StringBuilder> pool = new ObjectPool<>(2, () -> {
            StringBuilder sb = new StringBuilder();
            created.add(sb);
            return sb;
        }, sb -> { sb.setLength(0); resets.incrementAndGet(); });
        StringBuilder a = pool.acquire();
        a.append("x");
        assertTrue(pool.release(a));
        assertEquals(1, pool.idleCount());
        StringBuilder again = pool.acquire();
        assertSame(a, again);
        assertEquals(0, again.length());
        assertEquals(1, resets.get());
        assertThrows(NullPointerException.class, () -> pool.release(null));
        assertEquals(2, pool.maxIdle(), "maxIdle must report the configured bound");
    }

    @Test
    void tryParseMatchesJdkParsers() {
        java.util.Random r = new java.util.Random(24);
        String alphabet = "0123456789+-x ";
        String[] extremes = {"9223372036854775807", "9223372036854775808", "-9223372036854775808", "-9223372036854775809",
                "2147483647", "2147483648", "-2147483648", "-2147483649", "+0", "-0", "", "+", "-", "00012"};
        for (int trial = 0; trial < 200_000; trial++) {
            String s;
            if (trial < extremes.length) {
                s = extremes[trial];
            } else {
                StringBuilder sb = new StringBuilder();
                for (int k = r.nextInt(22); k > 0; k--) sb.append(alphabet.charAt(r.nextInt(r.nextInt(8) == 0 ? alphabet.length() : 12)));
                s = sb.toString();
            }
            java.util.OptionalLong l = space.nows.util.text.NumberParser.tryParseLong(s);
            Long jdkLong = parseOrNull(() -> Long.parseLong(s));
            assertEquals(jdkLong == null ? java.util.OptionalLong.empty() : java.util.OptionalLong.of(jdkLong), l, "long '" + s + "'");
            java.util.OptionalInt i = space.nows.util.text.NumberParser.tryParseInt(s);
            Integer jdkInt = parseOrNull(() -> Integer.parseInt(s));
            assertEquals(jdkInt == null ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(jdkInt), i, "int '" + s + "'");
        }
        assertTrue(space.nows.util.text.NumberParser.tryParseLong(null).isEmpty());
    }

    @Test
    void lerpIsExactAtEndpointsEvenWithInfiniteEndpoints() {
        assertEquals(0.0, space.nows.util.math.Interpolation.lerp(0.0, Double.POSITIVE_INFINITY, 0.0));
        assertEquals(Double.POSITIVE_INFINITY, space.nows.util.math.Interpolation.lerp(0.0, Double.POSITIVE_INFINITY, 1.0));
        assertEquals(0f, space.nows.util.math.Interpolation.lerp(0f, Float.POSITIVE_INFINITY, 0f));
    }

    private static <T> T parseOrNull(java.util.function.Supplier<T> parser) {
        try {
            return parser.get();
        } catch (NumberFormatException e) {
            return null;
        }
    }

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
