package space.nows.util.concurrent;

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

class QueuesEdgeTest {
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

}
