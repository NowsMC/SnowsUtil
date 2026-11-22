package space.nows.util.concurrent;

import space.nows.util.error.Checks;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Bounded lock-free single-producer / single-consumer queue. Exactly one thread may call the producer methods
 * ({@link #offer}) and exactly one thread may call the consumer methods ({@link #poll}, {@link #peek},
 * {@link #drain}, {@link #clear}); using it from more threads silently loses or duplicates elements.
 * The capacity is rounded up to a power of two. {@code null} elements are rejected.
 */
public final class SpscQueue<E> {
    private final Object[] buffer;
    private final int mask;
    private final AtomicLong head = new AtomicLong();
    private final AtomicLong tail = new AtomicLong();

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
        Checks.notNull(e, "e");
        long t = tail.get();
        if (t - head.get() == buffer.length) return false;
        buffer[(int) (t & mask)] = e;
        tail.lazySet(t + 1);
        return true;
    }

    /** Consumer side. Returns {@code null} if the queue is empty. */
    @SuppressWarnings("unchecked")
    public E poll() {
        long h = head.get();
        if (h == tail.get()) return null;
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
        if (h == tail.get()) return null;
        return (E) buffer[(int) (h & mask)];
    }

    /** Consumer side. Passes up to {@code max} elements to {@code sink}, in order; returns how many were consumed. */
    public int drain(Consumer<? super E> sink, int max) {
        Checks.notNull(sink, "sink");
        Checks.nonNegative(max, "max");
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

    /** Approximate when called from a thread other than the producer or consumer. */
    public int size() { return (int) (tail.get() - head.get()); }

    public int remainingCapacity() { return buffer.length - size(); }

    public boolean isEmpty() { return head.get() == tail.get(); }
}
