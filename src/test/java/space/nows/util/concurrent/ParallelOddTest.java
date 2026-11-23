package space.nows.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static org.junit.jupiter.api.Assertions.*;

class ParallelOddTest {
    @Test
    void forRangeVisitsEveryIndexExactlyOnce() {
        Random r = new Random(81);
        for (int trial = 0; trial < 200; trial++) {
            int start = r.nextInt(50) - 10;
            int end = start + r.nextInt(3000);
            int grain = 1 + r.nextInt(500);
            AtomicIntegerArray hits = new AtomicIntegerArray(Math.max(0, end - start));
            Parallel.forRange(start, end, grain, i -> hits.incrementAndGet(i - start));
            for (int i = 0; i < end - start; i++) assertEquals(1, hits.get(i), "index " + (start + i) + " grain " + grain);
        }
    }

    @Test
    void emptyAndReversedRangesDoNothing() {
        ConcurrentLinkedQueue<Integer> seen = new ConcurrentLinkedQueue<>();
        Parallel.forRange(5, 5, i -> seen.add(i));
        Parallel.forRange(9, 3, i -> seen.add(i));
        assertTrue(seen.isEmpty());
        assertEquals(0L, Parallel.sum(4, 4, i -> i));
    }

    @Test
    void grainMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> Parallel.forRange(0, 10, 0, i -> { }));
        assertThrows(IllegalArgumentException.class, () -> Parallel.forRange(0, 10, -3, i -> { }));
    }

    @Test
    void sumMatchesSequentialAccumulation() {
        Random r = new Random(82);
        for (int trial = 0; trial < 50; trial++) {
            int n = r.nextInt(20_000);
            int[] values = new int[n];
            for (int i = 0; i < n; i++) values[i] = r.nextInt();
            long expected = 0;
            for (int v : values) expected += v;
            long actual = Parallel.sum(0, n, i -> values[i]);
            assertEquals(expected, actual, "n=" + n);
        }
    }

    @Test
    void exceptionsInBodyPropagateToTheCaller() {
        assertThrows(IllegalStateException.class, () -> Parallel.forRange(0, 10_000, 100, i -> {
            if (i == 7777) throw new IllegalStateException("boom");
        }));
    }

    @Test
    void fillMatchesSequentialGeneration() {
        int[] a = new int[10_000];
        Parallel.fill(a, i -> i * 3 - 7);
        for (int i = 0; i < a.length; i++) assertEquals(i * 3 - 7, a[i]);
        Parallel.fill(new int[0], i -> 1);
    }
}
