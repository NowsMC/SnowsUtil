package space.nows.util.concurrent;

import space.nows.util.check.Preconditions;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;

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

    /** Sum of {@code mapper(i)} over {@code [startInclusive, endExclusive)}, wrapping on overflow. */
    public static long sum(int startInclusive, int endExclusive, IntToLongFunction mapper) {
        return reduceLong(ForkJoinPool.commonPool(), startInclusive, endExclusive, DEFAULT_GRAIN, 0L, mapper, Long::sum);
    }

    /**
     * Combines {@code mapper(i)} for every index in {@code [startInclusive, endExclusive)} with {@code combiner}.
     * {@code combiner} must be associative and {@code identity} must be its neutral element; results of adjacent
     * ranges are combined in index order, so it need not be commutative. Failure handling is the same as for
     * {@link #forRange}.
     */
    public static long reduceLong(ForkJoinPool pool, int startInclusive, int endExclusive, int grain, long identity,
                                  IntToLongFunction mapper, LongBinaryOperator combiner) {
        Preconditions.notNull(pool, "pool");
        Preconditions.notNull(mapper, "mapper");
        Preconditions.notNull(combiner, "combiner");
        Preconditions.positive(grain, "grain");
        if (endExclusive <= startInclusive) return identity;
        if (endExclusive - (long) startInclusive <= grain) {
            long acc = identity;
            for (int i = startInclusive; i < endExclusive; i++) acc = combiner.applyAsLong(acc, mapper.applyAsLong(i));
            return acc;
        }
        return pool.invoke(new ReduceTask(startInclusive, endExclusive, grain, identity, mapper, combiner, new AtomicBoolean()));
    }

    /** Sets {@code a[i] = generator(i)} for every index, in parallel. */
    public static void fill(int[] a, IntUnaryOperator generator) {
        Preconditions.notNull(a, "a");
        Preconditions.notNull(generator, "generator");
        forRange(0, a.length, i -> a[i] = generator.applyAsInt(i));
    }

    @SuppressWarnings("serial")
    private static final class ReduceTask extends RecursiveTask<Long> {
        private final int start, end, grain;
        private final long identity;
        private final IntToLongFunction mapper;
        private final LongBinaryOperator combiner;
        private final AtomicBoolean failed;

        ReduceTask(int start, int end, int grain, long identity, IntToLongFunction mapper, LongBinaryOperator combiner,
                   AtomicBoolean failed) {
            this.start = start; this.end = end; this.grain = grain; this.identity = identity;
            this.mapper = mapper; this.combiner = combiner; this.failed = failed;
        }

        @Override protected Long compute() {
            if (failed.get()) return identity;
            long len = (long) end - start;
            if (len <= grain) {
                try {
                    long acc = identity;
                    for (int i = start; i < end; i++) acc = combiner.applyAsLong(acc, mapper.applyAsLong(i));
                    return acc;
                } catch (Throwable t) {
                    failed.set(true);
                    throw t;
                }
            }
            int mid = (int) (start + (len >>> 1));
            ReduceTask left = new ReduceTask(start, mid, grain, identity, mapper, combiner, failed);
            ReduceTask right = new ReduceTask(mid, end, grain, identity, mapper, combiner, failed);
            left.fork();
            long r = right.compute();
            long l = left.join();
            return combiner.applyAsLong(l, r);
        }
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
            long len = (long) end - start;
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
