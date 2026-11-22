package space.nows.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SpscQueueSizeTest {
    @Test void sizeStaysWithinCapacityWhileOtherThreadsRun() throws InterruptedException {
        SpscQueue<Integer> q = new SpscQueue<>(8);
        AtomicBoolean done = new AtomicBoolean();
        int total = 200_000;
        Thread producer = new Thread(() -> {
            for (int i = 0; i < total; i++) while (!q.offer(i)) Thread.onSpinWait();
        });
        Thread consumer = new Thread(() -> {
            int got = 0;
            while (got < total) if (q.poll() != null) got++;
            done.set(true);
        });
        producer.start();
        consumer.start();
        while (!done.get()) {
            int size = q.size();
            assertTrue(size >= 0 && size <= q.capacity(), "size " + size);
            int remaining = q.remainingCapacity();
            assertTrue(remaining >= 0 && remaining <= q.capacity(), "remaining " + remaining);
        }
        producer.join();
        consumer.join();
        assertTrue(q.isEmpty());
    }
}
