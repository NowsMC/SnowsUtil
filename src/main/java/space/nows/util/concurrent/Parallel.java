package space.nows.util.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.function.IntConsumer;

public final class Parallel {
    private Parallel() {}

    public static final int DEFAULT_GRAIN = 1024;

    public static void forRange(int startInclusive, int endExclusive, IntConsumer body) {
        forRange(startInclusive, endExclusive, DEFAULT_GRAIN, body);
    }

    public static void forRange(int startInclusive, int endExclusive, int grain, IntConsumer body) {
        if (grain < 1) throw new IllegalArgumentException("grain < 1");
        if (endExclusive <= startInclusive) return;
        if (endExclusive - startInclusive <= grain) {
            for (int i = startInclusive; i < endExclusive; i++) body.accept(i);
            return;
        }
        ForkJoinPool.commonPool().invoke(new RangeTask(startInclusive, endExclusive, grain, body));
    }

    @SuppressWarnings("serial")
    private static final class RangeTask extends RecursiveAction {
        private final int start, end, grain;
        private final IntConsumer body;

        RangeTask(int start, int end, int grain, IntConsumer body) {
            this.start = start; this.end = end; this.grain = grain; this.body = body;
        }

        @Override protected void compute() {
            int len = end - start;
            if (len <= grain) {
                for (int i = start; i < end; i++) body.accept(i);
                return;
            }
            int mid = start + (len >>> 1);
            invokeAll(new RangeTask(start, mid, grain, body), new RangeTask(mid, end, grain, body));
        }
    }
}
