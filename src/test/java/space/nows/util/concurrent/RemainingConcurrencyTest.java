package space.nows.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;

import static org.junit.jupiter.api.Assertions.*;

class RemainingConcurrencyTest {
    private static final ForkJoinPool POOL = new ForkJoinPool(4);

    @Test
    void backoffProgressesThroughAllPhasesWithoutOverflowing() {
        Backoff backoff = new Backoff();
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(20), () -> {
            for (int i = 0; i < 400; i++) backoff.pause();
        });
        backoff.reset();
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> backoff.pause());
    }

    @Test
    void namedThreadFactoryNamesDaemonsAndPriorities() {
        ThreadFactory f = new NamedThreadFactory("worker");
        Thread a = f.newThread(() -> { });
        Thread b = f.newThread(() -> { });
        assertTrue(a.getName().startsWith("worker"));
        assertNotEquals(a.getName(), b.getName(), "each thread gets its own name");
        assertTrue(a.isDaemon());
        assertEquals(Thread.NORM_PRIORITY, a.getPriority());
        Thread custom = new NamedThreadFactory("svc", false, Thread.MAX_PRIORITY).newThread(() -> { });
        assertFalse(custom.isDaemon());
        assertEquals(Thread.MAX_PRIORITY, custom.getPriority());
        assertThrows(IllegalArgumentException.class, () -> new NamedThreadFactory("x", true, 0));
    }

    @Test
    void reduceLongMatchesSequentialForAssociativeOperations() throws Exception {
        { ForkJoinPool pool = POOL;
            long expectedSum = 0;
            for (int i = 0; i < 50_000; i++) expectedSum += i * 3L;
            long sum = Parallel.reduceLong(pool, 0, 50_000, 97, 0L, i -> i * 3L, Long::sum);
            assertEquals(expectedSum, sum);
            long max = Parallel.reduceLong(pool, 0, 1000, 10, Long.MIN_VALUE, i -> (i * 37) % 1000, Math::max);
            assertEquals(999L, max);
            assertEquals(42L, Parallel.reduceLong(pool, 5, 5, 3, 42L, i -> 1L, Long::sum), "empty range yields identity");
        }
    }

    @Test
    void reduceLongRejectsBadArguments() {
        { ForkJoinPool pool = POOL;
            assertThrows(IllegalArgumentException.class, () -> Parallel.reduceLong(pool, 0, 10, 0, 0L, i -> 1L, Long::sum));
            assertThrows(NullPointerException.class, () -> Parallel.reduceLong(null, 0, 10, 1, 0L, i -> 1L, Long::sum));
        }
    }
}
