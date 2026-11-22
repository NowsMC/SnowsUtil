package space.nows.util.concurrent;

import space.nows.util.check.Preconditions;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

/**
 * Fork/join parallel loops over an {@code int} range. An exception thrown by the body is rethrown to the caller
 * (the JDK may re-create it as a new exception of the same type). Once any chunk has failed, chunks that have not
 * started yet are skipped; chunks that are already running finish their current {@code grain}-sized range.
 */
public final class Parallel {
    private Parallel() {}

    public static final int DEFAULT_GRAIN = 1024;

    public static void forRange(int startInclusive, int endExclusive, IntConsumer body) {
        forRange(ForkJoinPool.commonPool(), startInclusive, endExclusive, DEFAULT_GRAIN, body);
    }

    public static void forRange(int startInclusive, int endExclusive, int grain, IntConsumer body) {
        forRange(ForkJoinPool.commonPool(), startInclusive, endExclusive, grain, body);
    }

    /** Runs {@code body} for every index in {@code [startInclusive, endExclusive)} using the given pool. */
    public static void forRange(ForkJoinPool pool, int startInclusive, int endExclusive, int grain, IntConsumer body) {
        Preconditions.notNull(pool, "pool");
        Preconditions.notNull(body, "body");
        Preconditions.positive(grain, "grain");
        if (endExclusive <= startInclusive) return;
        if (endExclusive - (long) startInclusive <= grain) {
            for (int i = startInclusive; i < endExclusive; i++) body.accept(i);
            return;
        }
        pool.invoke(new RangeTask(startInclusive, endExclusive, grain, body, new AtomicBoolean()));
    }

    @SuppressWarnings("serial")
    private static final class RangeTask extends RecursiveAction {
        private final int start, end, grain;
        private final IntConsumer body;
        private final AtomicBoolean failed;

        RangeTask(int start, int end, int grain, IntConsumer body, AtomicBoolean failed) {
            this.start = start; this.end = end; this.grain = grain; this.body = body; this.failed = failed;
        }

        @Override protected void compute() {
            if (failed.get()) return;
            long len = (long) end - start;   // may exceed Integer.MAX_VALUE
            if (len <= grain) {
                try {
                    for (int i = start; i < end; i++) body.accept(i);
                } catch (Throwable t) {
                    failed.set(true);
                    throw t;
                }
                return;
            }
            int mid = (int) (start + (len >>> 1));
            invokeAll(new RangeTask(start, mid, grain, body, failed), new RangeTask(mid, end, grain, body, failed));
        }
    }
}
