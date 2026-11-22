package space.nows.util.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyExtrasTest {
    @Test void mpmcSingleThreadedSemantics() {
        MpmcQueue<Integer> q = new MpmcQueue<>(1);
        assertEquals(2, q.capacity());
        assertTrue(q.isEmpty());
        assertNull(q.poll());
        assertTrue(q.offer(1));
        assertTrue(q.offer(2));
        assertFalse(q.offer(3));
        assertEquals(2, q.size());
        assertEquals(0, q.remainingCapacity());
        assertEquals(1, q.poll());
        assertTrue(q.offer(3));
        assertEquals(2, q.poll());
        assertEquals(3, q.poll());
        assertNull(q.poll());
        assertThrows(NullPointerException.class, () -> q.offer(null));
        assertThrows(IllegalArgumentException.class, () -> new MpmcQueue<String>(0));
        assertEquals(8, new MpmcQueue<String>(5).capacity());
        MpmcQueue<Integer> d = new MpmcQueue<>(16);
        for (int i = 0; i < 10; i++) d.offer(i);
        List<Integer> got = new ArrayList<>();
        assertEquals(4, d.drain(got::add, 4));
        assertEquals(List.of(0, 1, 2, 3), got);
        assertEquals(6, d.drain(got::add, 100));
    }

    @Test void mpmcDeliversEveryElementExactlyOnce() {
        int producers = 4, consumers = 4, perProducer = 50_000, total = producers * perProducer;
        MpmcQueue<Integer> q = new MpmcQueue<>(64);
        AtomicIntegerArray seen = new AtomicIntegerArray(total);
        AtomicInteger consumed = new AtomicInteger();
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            List<Thread> threads = new ArrayList<>();
            for (int p = 0; p < producers; p++) {
                int base = p * perProducer;
                threads.add(new Thread(() -> {
                    Backoff backoff = new Backoff();
                    for (int i = 0; i < perProducer; i++) {
                        while (!q.offer(base + i)) backoff.pause();
                        backoff.reset();
                    }
                }));
            }
            for (int c = 0; c < consumers; c++) {
                threads.add(new Thread(() -> {
                    Backoff backoff = new Backoff();
                    while (consumed.get() < total) {
                        Integer v = q.poll();
                        if (v == null) {
                            backoff.pause();
                            continue;
                        }
                        backoff.reset();
                        seen.incrementAndGet(v);
                        consumed.incrementAndGet();
                    }
                }));
            }
            threads.forEach(Thread::start);
            for (Thread t : threads) t.join();
        });
        for (int i = 0; i < total; i++) assertEquals(1, seen.get(i), "element " + i);
        assertTrue(q.isEmpty());
    }

    @Test void mpmcKeepsPerProducerOrder() throws InterruptedException {
        MpmcQueue<long[]> q = new MpmcQueue<>(32);
        int perProducer = 20_000;
        Thread[] producers = new Thread[2];
        for (int p = 0; p < producers.length; p++) {
            int id = p;
            producers[p] = new Thread(() -> {
                for (int i = 0; i < perProducer; i++) while (!q.offer(new long[] {id, i})) Thread.onSpinWait();
            });
            producers[p].start();
        }
        long[] next = new long[2];
        int got = 0;
        while (got < perProducer * 2) {
            long[] v = q.poll();
            if (v == null) continue;
            assertEquals(next[(int) v[0]]++, v[1]);
            got++;
        }
        for (Thread t : producers) t.join();
    }

    @Test void objectPoolReusesAndResets() {
        AtomicInteger created = new AtomicInteger();
        AtomicInteger resets = new AtomicInteger();
        ObjectPool<StringBuilder> pool = new ObjectPool<>(2, () -> {
            created.incrementAndGet();
            return new StringBuilder();
        }, sb -> {
            resets.incrementAndGet();
            sb.setLength(0);
        });
        StringBuilder a = pool.acquire();
        a.append("dirty");
        assertTrue(pool.release(a));
        assertEquals(1, pool.idleCount());
        StringBuilder again = pool.acquire();
        assertSame(a, again);
        assertEquals(0, again.length());
        assertEquals(1, created.get());
        StringBuilder b = pool.acquire();
        StringBuilder c = pool.acquire();
        assertTrue(pool.release(again));
        assertTrue(pool.release(b));
        assertFalse(pool.release(c));
        assertEquals(4, resets.get());
        assertEquals(2, pool.maxIdle());
        assertThrows(NullPointerException.class, () -> pool.release(null));
    }

    @Test void namedThreadFactory() {
        NamedThreadFactory f = new NamedThreadFactory("worker", false, Thread.MAX_PRIORITY);
        Thread t1 = f.newThread(() -> { });
        Thread t2 = f.newThread(() -> { });
        assertEquals("worker-1", t1.getName());
        assertEquals("worker-2", t2.getName());
        assertFalse(t1.isDaemon());
        assertEquals(Thread.MAX_PRIORITY, t1.getPriority());
        assertTrue(new NamedThreadFactory("d").newThread(() -> { }).isDaemon());
        assertThrows(IllegalArgumentException.class, () -> new NamedThreadFactory("", true, 5));
        assertThrows(IllegalArgumentException.class, () -> new NamedThreadFactory("x", true, 11));
    }

    @Test void backoffEscalatesWithoutHanging() {
        Backoff b = new Backoff();
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            for (int i = 0; i < 140; i++) b.pause();
            b.reset();
            b.pause();
        });
    }

    @Test void parallelSumAndReduce() {
        long expected = 0;
        for (int i = 0; i < 100_000; i++) expected += (long) i * i;
        assertEquals(expected, Parallel.sum(0, 100_000, i -> (long) i * i));
        assertEquals(0L, Parallel.sum(5, 5, i -> 1));
        assertEquals(99L, Parallel.sum(0, 100, i -> 1) - 1);
        assertEquals(5L, Parallel.reduceLong(ForkJoinPool.commonPool(), 5, 50_000, 64, -1L, i -> i,
                (a, b) -> a == -1L ? b : a));
        assertEquals(49_999L, Parallel.reduceLong(ForkJoinPool.commonPool(), 5, 50_000, 64, 0L, i -> i, Math::max));
        assertThrows(IllegalArgumentException.class, () -> Parallel.reduceLong(ForkJoinPool.commonPool(), 0, 10, 0, 0L, i -> i, Long::sum));
        assertThrows(NullPointerException.class, () -> Parallel.sum(0, 10, null));
    }

    @Test void parallelSumPropagatesFailures() {
        assertThrows(IllegalStateException.class, () -> Parallel.sum(0, 100_000, i -> {
            if (i == 77_777) throw new IllegalStateException("boom");
            return i;
        }));
    }

    @Test void parallelFill() {
        int[] a = new int[50_000];
        Parallel.fill(a, i -> i * 2);
        for (int i = 0; i < a.length; i++) assertEquals(i * 2, a[i]);
        assertThrows(NullPointerException.class, () -> Parallel.fill(null, i -> i));
    }
}
