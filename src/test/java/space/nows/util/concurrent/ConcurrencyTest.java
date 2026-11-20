package space.nows.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {
    @Test void concurrency() throws Exception {
        SpscQueue<Integer> q = new SpscQueue<>(8);
        assertEquals(8, q.capacity());
        for (int i = 0; i < 8; i++) assertTrue(q.offer(i));
        assertFalse(q.offer(99));
        assertEquals(0, q.poll());

        final int n = 200_000;
        SpscQueue<Integer> pipe = new SpscQueue<>(1024);
        Thread producer = new Thread(() -> {
            for (int i = 0; i < n; i++) while (!pipe.offer(i)) Thread.onSpinWait();
        });
        producer.start();
        for (int expected = 0; expected < n; ) {
            Integer v = pipe.poll();
            if (v == null) { Thread.onSpinWait(); continue; }
            assertEquals(expected++, v);
        }
        producer.join();

        AtomicInteger sum = new AtomicInteger();
        Parallel.forRange(0, 100_000, 100, sum::addAndGet);
        assertEquals((int) (99_999L * 100_000L / 2), sum.get());
    }
}
