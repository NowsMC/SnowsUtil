package space.nows.util.collections;

import space.nows.util.check.Preconditions;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

/** Double-ended queue of {@code int} values in a circular array, with no boxing. Not thread-safe. */
public class IntArrayDeque {
    private int[] data;
    private int head;
    private int size;
    private int modCount;

    public IntArrayDeque() { this(16); }

    public IntArrayDeque(int expectedSize) {
        data = new int[Growth.powerOfTwo(Preconditions.nonNegative(expectedSize, "expectedSize"))];
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    private void grow() {
        int[] old = data;
        int[] bigger = new int[Growth.powerOfTwo(old.length + 1)];
        int firstRun = Math.min(size, old.length - head);
        System.arraycopy(old, head, bigger, 0, firstRun);
        System.arraycopy(old, 0, bigger, firstRun, size - firstRun);
        data = bigger;
        head = 0;
    }

    public void addFirst(int value) {
        if (size == data.length) grow();
        head = (head - 1) & (data.length - 1);
        data[head] = value;
        size++;
        modCount++;
    }

    public void addLast(int value) {
        if (size == data.length) grow();
        data[(head + size) & (data.length - 1)] = value;
        size++;
        modCount++;
    }

    public int removeFirst() {
        if (size == 0) throw new NoSuchElementException("empty deque");
        int v = data[head];
        head = (head + 1) & (data.length - 1);
        size--;
        modCount++;
        return v;
    }

    public int removeLast() {
        if (size == 0) throw new NoSuchElementException("empty deque");
        size--;
        modCount++;
        return data[(head + size) & (data.length - 1)];
    }

    public int first() {
        if (size == 0) throw new NoSuchElementException("empty deque");
        return data[head];
    }

    public int last() {
        if (size == 0) throw new NoSuchElementException("empty deque");
        return data[(head + size - 1) & (data.length - 1)];
    }

    /** Element at logical position {@code index}, counted from the front. */
    public int get(int index) {
        Objects.checkIndex(index, size);
        return data[(head + index) & (data.length - 1)];
    }

    public void clear() {
        head = 0;
        size = 0;
        modCount++;
    }

    public int[] toArray() {
        int[] out = new int[size];
        int firstRun = Math.min(size, data.length - head);
        System.arraycopy(data, head, out, 0, firstRun);
        System.arraycopy(data, 0, out, firstRun, size - firstRun);
        return out;
    }

    public void forEach(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        int mask = data.length - 1;
        for (int i = 0, n = size; i < n; i++) {
            action.accept(data[(head + i) & mask]);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
    }

    /** Fail-fast iterator from front to back; {@code remove()} is not supported. */
    public PrimitiveIterator.OfInt iterator() {
        return new PrimitiveIterator.OfInt() {
            private final int expected = modCount;
            private int next;

            @Override public boolean hasNext() { return next < size; }

            @Override public int nextInt() {
                if (expected != modCount) throw new ConcurrentModificationException();
                if (next >= size) throw new NoSuchElementException();
                return data[(head + next++) & (data.length - 1)];
            }
        };
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof IntArrayDeque d && d.size == size && Arrays.equals(toArray(), d.toArray());
    }

    @Override public int hashCode() { return Arrays.hashCode(toArray()); }

    @Override public String toString() { return Arrays.toString(toArray()); }
}
