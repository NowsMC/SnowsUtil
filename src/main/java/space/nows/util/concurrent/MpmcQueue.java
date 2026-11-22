package space.nows.util.concurrent;

import space.nows.util.check.Preconditions;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Consumer;

/**
 * Bounded lock-free multi-producer / multi-consumer queue (Vyukov's array queue). Any number of threads may call
 * any method. The capacity is rounded up to a power of two, with a minimum of 2. {@code null} elements are rejected.
 * Elements are delivered in FIFO order per producer; {@link #offer} may report full slightly before the queue
 * holds {@link #capacity()} elements while a consumer is still releasing a slot.
 */
public final class MpmcQueue<E> {
    private final Object[] buffer;
    private final AtomicLongArray sequence;
    private final int mask;
    private final PaddedAtomicLong enqueuePos = new PaddedAtomicLong();
    private final PaddedAtomicLong dequeuePos = new PaddedAtomicLong();

    public MpmcQueue(int capacity) {
        if (capacity < 1 || capacity > (1 << 30)) {
            throw new IllegalArgumentException("capacity must be in [1, 2^30] but was " + capacity);
        }
        int cap = 2;
        while (cap < capacity) cap <<= 1;
        buffer = new Object[cap];
        sequence = new AtomicLongArray(cap);
        for (int i = 0; i < cap; i++) sequence.lazySet(i, i);
        mask = cap - 1;
    }

    public int capacity() { return buffer.length; }

    /** Returns {@code false} if the queue is full. */
    public boolean offer(E e) {
        Preconditions.notNull(e, "e");
        long pos = enqueuePos.get();
        for (;;) {
            int idx = (int) (pos & mask);
            long diff = sequence.get(idx) - pos;
            if (diff == 0) {
                if (enqueuePos.compareAndSet(pos, pos + 1)) {
                    buffer[idx] = e;
                    sequence.lazySet(idx, pos + 1);
                    return true;
                }
            } else if (diff < 0) {
                return false;
            }
            pos = enqueuePos.get();
        }
    }

    /** Returns {@code null} if the queue is empty. */
    @SuppressWarnings("unchecked")
    public E poll() {
        long pos = dequeuePos.get();
        for (;;) {
            int idx = (int) (pos & mask);
            long diff = sequence.get(idx) - (pos + 1);
            if (diff == 0) {
                if (dequeuePos.compareAndSet(pos, pos + 1)) {
                    E e = (E) buffer[idx];
                    buffer[idx] = null;
                    sequence.lazySet(idx, pos + mask + 1);
                    return e;
                }
            } else if (diff < 0) {
                return null;
            }
            pos = dequeuePos.get();
        }
    }

    /** Passes up to {@code max} elements to {@code sink}, in order; returns how many were consumed. */
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

    /** Approximate while other threads are running; always within {@code [0, capacity()]}. */
    public int size() {
        long head = dequeuePos.get();
        long tail = enqueuePos.get();
        return (int) Math.max(0L, Math.min(tail - head, buffer.length));
    }

    public int remainingCapacity() { return buffer.length - size(); }

    public boolean isEmpty() { return size() == 0; }
}
