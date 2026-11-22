package space.nows.util.concurrent;

import space.nows.util.check.Preconditions;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Bounded lock-free single-producer / single-consumer queue. Exactly one thread may call the producer methods
 * ({@link #offer}) and exactly one thread may call the consumer methods ({@link #poll}, {@link #peek},
 * {@link #drain}, {@link #clear}); using it from more threads silently loses or duplicates elements.
 * The capacity is rounded up to a power of two. {@code null} elements are rejected.
 */
public final class SpscQueue<E> {
    /** A position counter padded onto its own cache line, plus the owning thread's cached copy of the other counter. */
    @SuppressWarnings({"serial", "unused"})
    private static final class Cursor extends AtomicLong {
        long cachedOther;
        private long p1, p2, p3, p4, p5, p6;
    }

    private final Object[] buffer;
    private final int mask;
    private final Cursor head = new Cursor();
    private final Cursor tail = new Cursor();

    public SpscQueue(int capacity) {
        if (capacity < 1 || capacity > (1 << 30)) {
            throw new IllegalArgumentException("capacity must be in [1, 2^30] but was " + capacity);
        }
        int cap = 1;
        while (cap < capacity) cap <<= 1;
        buffer = new Object[cap];
        mask = cap - 1;
    }

    public int capacity() { return buffer.length; }

    /** Producer side. Returns {@code false} if the queue is full. */
    public boolean offer(E e) {
        Preconditions.notNull(e, "e");
        long t = tail.get();
        if (t - tail.cachedOther == buffer.length) {
            tail.cachedOther = head.get();
            if (t - tail.cachedOther == buffer.length) return false;
        }
        buffer[(int) (t & mask)] = e;
        tail.lazySet(t + 1);
        return true;
    }

    /** Consumer side. Returns {@code null} if the queue is empty. */
    @SuppressWarnings("unchecked")
    public E poll() {
        long h = head.get();
        if (h == head.cachedOther) {
            head.cachedOther = tail.get();
            if (h == head.cachedOther) return null;
        }
        int idx = (int) (h & mask);
        E e = (E) buffer[idx];
        buffer[idx] = null;
        head.lazySet(h + 1);
        return e;
    }

    /** Consumer side. The next element without removing it, or {@code null}. */
    @SuppressWarnings("unchecked")
    public E peek() {
        long h = head.get();
        if (h == head.cachedOther) {
            head.cachedOther = tail.get();
            if (h == head.cachedOther) return null;
        }
        return (E) buffer[(int) (h & mask)];
    }

    /** Consumer side. Passes up to {@code max} elements to {@code sink}, in order; returns how many were consumed. */
    public int drain(Consumer<? super E> sink, int max) {
        Preconditions.notNull(sink, "sink");
        Preconditions.nonNegative(max, "max");
        int n = 0;
        E e;
        while (n < max && (e = poll()) != null) {
            sink.accept(e);
            n++;
        }
        return n;
    }

    /** Consumer side. Removes everything currently queued. */
    public void clear() {
        while (poll() != null) { /* discard */ }
    }

    /** Approximate when called from a thread other than the producer or consumer; always within {@code [0, capacity()]}. */
    public int size() {
        long h = head.get();
        long t = tail.get();
        return (int) Math.min(t - h, buffer.length);
    }

    public int remainingCapacity() { return buffer.length - size(); }

    public boolean isEmpty() { return head.get() == tail.get(); }
}
