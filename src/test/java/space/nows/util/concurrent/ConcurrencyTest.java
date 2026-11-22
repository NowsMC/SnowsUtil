package space.nows.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {
    // ---------------------------------------------------------------- SpscQueue
    @Test void singleThreadedSemantics() {
        SpscQueue<Integer> q = new SpscQueue<>(5);
        assertEquals(8, q.capacity());                                    // rounded up to a power of two
        assertTrue(q.isEmpty());
        assertNull(q.poll());
        assertNull(q.peek());
        for (int i = 0; i < 8; i++) assertTrue(q.offer(i));
        assertFalse(q.offer(99));                                         // full
        assertEquals(8, q.size());
        assertEquals(0, q.remainingCapacity());
        assertEquals(0, q.peek());
        assertEquals(0, q.peek());                                        // peek does not remove
        for (int i = 0; i < 8; i++) assertEquals(i, q.poll());
        assertNull(q.poll());
        assertTrue(q.isEmpty());
    }

    @Test void drainAndClear() {
        SpscQueue<Integer> q = new SpscQueue<>(16);
        for (int i = 0; i < 10; i++) q.offer(i);
        List<Integer> got = new ArrayList<>();
        assertEquals(4, q.drain(got::add, 4));
        assertEquals(List.of(0, 1, 2, 3), got);
        assertEquals(6, q.size());
        assertEquals(0, q.drain(got::add, 0));
        assertEquals(6, q.drain(got::add, 100));                          // stops when the queue runs dry
        assertEquals(10, got.size());
        for (int i = 0; i < 5; i++) q.offer(i);
        q.clear();
        assertTrue(q.isEmpty());
        assertEquals(16, q.remainingCapacity());
        assertThrows(NullPointerException.class, () -> q.drain(null, 1));
        assertThrows(IllegalArgumentException.class, () -> q.drain(x -> { }, -1));
    }

    @Test void wrapsAroundManyTimes() {
        SpscQueue<Integer> q = new SpscQueue<>(4);
        int next = 0, expected = 0;
        for (int i = 0; i < 1_000; i++) {
            while (q.offer(next)) next++;
            for (int k = 0; k < 3; k++) assertEquals(expected++, q.poll());
        }
    }

    @Test void constructorAndElementGuards() {
        assertThrows(IllegalArgumentException.class, () -> new SpscQueue<String>(0));
        assertThrows(IllegalArgumentException.class, () -> new SpscQueue<String>(-1));
        assertThrows(IllegalArgumentException.class, () -> new SpscQueue<String>((1 << 30) + 1));
        assertEquals(1, new SpscQueue<String>(1).capacity());
        SpscQueue<String> q = new SpscQueue<>(2);
        NullPointerException e = assertThrows(NullPointerException.class, () -> q.offer(null));
        assertTrue(e.getMessage().contains("e"));
        assertTrue(q.isEmpty());
    }

    @Test void producerAndConsumerOnTwoThreadsPreserveOrderAndCount() throws Exception {
        final int n = 300_000;
        SpscQueue<Integer> q = new SpscQueue<>(1024);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread producer = new Thread(() -> {
            for (int i = 0; i < n; i++) while (!q.offer(i)) Thread.onSpinWait();
        });
        Thread consumer = new Thread(() -> {
            try {
                for (int expected = 0; expected < n; expected++) {
                    Integer v;
                    while ((v = q.poll()) == null) Thread.onSpinWait();
                    if (v != expected) throw new AssertionError("expected " + expected + " but got " + v);
                }
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        producer.start();
        consumer.start();
        producer.join(30_000);
        consumer.join(30_000);
        assertFalse(producer.isAlive() || consumer.isAlive());
        assertNull(failure.get());
        assertTrue(q.isEmpty());
    }

    // ---------------------------------------------------------------- Parallel
    @Test void everyIndexIsVisitedExactlyOnce() {
        int n = 100_000;
        int[] hits = new int[n];
        Parallel.forRange(0, n, 128, i -> hits[i]++);
        for (int h : hits) assertEquals(1, h);
        java.util.Arrays.fill(hits, 0);
        Parallel.forRange(0, n, i -> hits[i] += 1);
        for (int h : hits) assertEquals(1, h);
    }


    @Test void offsetRangesAndNegativeStarts() {
        AtomicInteger sum = new AtomicInteger();
        Parallel.forRange(-500, 500, 16, sum::addAndGet);
        assertEquals(-500, sum.get());                                    // sum of -500..499
        AtomicInteger calls = new AtomicInteger();
        Parallel.forRange(5, 5, 4, i -> calls.incrementAndGet());
        Parallel.forRange(9, 3, 4, i -> calls.incrementAndGet());
        assertEquals(0, calls.get());
    }

    @Test void wideRangesAreSplitInsteadOfRunSequentially() {
        // [-2e9, 2e9) is wider than Integer.MAX_VALUE. If the width overflowed, the range would be mistaken for a small one
        // and run sequentially from -2e9, needing ~2e9 calls to reach index 0. Split correctly, the right half starts at once.
        ForkJoinPool pool = new ForkJoinPool(4);
        LongAdder calls = new LongAdder();
        try {
            assertThrows(IllegalStateException.class, () -> Parallel.forRange(pool, -2_000_000_000, 2_000_000_000, 1 << 20, i -> {
                if (i >= 0) throw new IllegalStateException("reached the upper half");
                calls.increment();
                if (calls.sum() > 100_000_000L) throw new AssertionError("range was run sequentially");
            }));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test void customPoolIsUsed() {
        ForkJoinPool pool = new ForkJoinPool(3);
        try {
            AtomicInteger count = new AtomicInteger();
            AtomicReference<Boolean> inPool = new AtomicReference<>(true);
            Parallel.forRange(pool, 0, 50_000, 64, i -> {
                count.incrementAndGet();
                if (Thread.currentThread() instanceof java.util.concurrent.ForkJoinWorkerThread w && w.getPool() != pool) inPool.set(false);
            });
            assertEquals(50_000, count.get());
            assertTrue(inPool.get());
        } finally {
            pool.shutdown();
        }
    }

    @Test void bodyExceptionsReachTheCaller() {
        assertThrows(IllegalStateException.class, () -> Parallel.forRange(0, 100_000, 64, i -> {
            if (i == 5_000) throw new IllegalStateException("boom");
        }));
        assertThrows(IllegalArgumentException.class, () -> Parallel.forRange(0, 10, 1000, i -> {          // sequential fast path too
            if (i == 3) throw new IllegalArgumentException("small");
        }));
    }

    @Test void afterAFailureChunksThatHaveNotStartedAreSkipped() {
        ForkJoinPool pool = new ForkJoinPool(4);
        AtomicInteger calls = new AtomicInteger();
        try {
            // the failure is at the start of the RIGHT half; the left half is deliberately slow, so without skipping
            // it would run all 5,000,000 of its iterations before the exception could reach the caller
            assertThrows(IllegalStateException.class, () -> Parallel.forRange(pool, 0, 10_000_000, 1_000, i -> {
                calls.incrementAndGet();
                if (i == 5_000_000) throw new IllegalStateException("fail in the right half");
                if (i < 5_000_000) Thread.onSpinWait();
            }));
        } finally {
            pool.shutdownNow();
        }
        assertTrue(calls.get() < 1_000_000, "ran " + calls.get() + " of 10000000 iterations after the failure");
    }

    @Test void argumentGuards() {
        assertThrows(IllegalArgumentException.class, () -> Parallel.forRange(0, 10, 0, i -> { }));
        assertThrows(IllegalArgumentException.class, () -> Parallel.forRange(0, 10, -5, i -> { }));
        assertThrows(NullPointerException.class, () -> Parallel.forRange(0, 10, 4, null));
        assertThrows(NullPointerException.class, () -> Parallel.forRange(null, 0, 10, 4, i -> { }));
    }
}
