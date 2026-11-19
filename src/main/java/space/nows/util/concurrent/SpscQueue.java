package space.nows.util.concurrent;

import java.util.concurrent.atomic.AtomicLong;

public final class SpscQueue<E> {
    private final Object[] buffer;
    private final int mask;
    private final AtomicLong head = new AtomicLong();
    private final AtomicLong tail = new AtomicLong();

    public SpscQueue(int capacity) {
        if (capacity < 1 || capacity > (1 << 30)) throw new IllegalArgumentException("capacity: " + capacity);
        int cap = 1;
        while (cap < capacity) cap <<= 1;
        buffer = new Object[cap];
        mask = cap - 1;
    }

    public int capacity() { return buffer.length; }

    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        long t = tail.get();
        if (t - head.get() == buffer.length) return false;
        buffer[(int) (t & mask)] = e;
        tail.lazySet(t + 1);
        return true;
    }

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

    public int size() { return (int) (tail.get() - head.get()); }

    public boolean isEmpty() { return head.get() == tail.get(); }
}
