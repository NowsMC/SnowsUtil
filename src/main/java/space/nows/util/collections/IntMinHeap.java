package space.nows.util.collections;

import space.nows.util.check.Preconditions;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Binary min-heap of {@code int} values with no boxing; {@link #poll()} returns the smallest. Not thread-safe. */
public class IntMinHeap {
    private int[] data;
    private int size;

    public IntMinHeap() { this(16); }

    public IntMinHeap(int expectedSize) {
        data = new int[Math.max(1, Preconditions.nonNegative(expectedSize, "expectedSize"))];
    }

    /** Builds a heap from {@code values} in linear time. */
    public static IntMinHeap of(int... values) {
        Objects.requireNonNull(values, "values");
        IntMinHeap heap = new IntMinHeap(values.length);
        System.arraycopy(values, 0, heap.data, 0, values.length);
        heap.size = values.length;
        for (int i = (heap.size >>> 1) - 1; i >= 0; i--) heap.siftDown(i, heap.data[i]);
        return heap;
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    public void add(int value) {
        if (size == data.length) data = Arrays.copyOf(data, Growth.arrayLength(data.length, size + 1));
        siftUp(size++, value);
    }

    /** Smallest value; throws {@link NoSuchElementException} if empty. */
    public int peek() {
        if (size == 0) throw new NoSuchElementException("empty heap");
        return data[0];
    }

    /** Removes and returns the smallest value; throws {@link NoSuchElementException} if empty. */
    public int poll() {
        if (size == 0) throw new NoSuchElementException("empty heap");
        int top = data[0];
        int last = data[--size];
        if (size > 0) siftDown(0, last);
        return top;
    }

    public void clear() { size = 0; }

    /** Contents in heap order, not sorted. */
    public int[] toArray() { return Arrays.copyOf(data, size); }

    private void siftUp(int index, int value) {
        int[] d = data;
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            int p = d[parent];
            if (value >= p) break;
            d[index] = p;
            index = parent;
        }
        d[index] = value;
    }

    private void siftDown(int index, int value) {
        int[] d = data;
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            int c = d[child];
            int right = child + 1;
            if (right < size && d[right] < c) c = d[child = right];
            if (value <= c) break;
            d[index] = c;
            index = child;
        }
        d[index] = value;
    }

    @Override public String toString() { return Arrays.toString(toArray()); }
}
