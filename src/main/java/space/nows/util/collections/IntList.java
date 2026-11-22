package space.nows.util.collections;

import space.nows.util.check.Preconditions;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/** Growable {@code int} array list with no boxing. Not thread-safe. */
public class IntList {
    private static final int[] EMPTY = {};

    private int[] data;
    private int size;
    private int modCount;

    public IntList() { data = EMPTY; }

    public IntList(int initialCapacity) {
        data = Preconditions.nonNegative(initialCapacity, "initialCapacity") == 0 ? EMPTY : new int[initialCapacity];
    }

    public static IntList of(int... values) {
        Objects.requireNonNull(values, "values");
        IntList list = new IntList(values.length);
        list.addAll(values);
        return list;
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    public int capacity() { return data.length; }

    public int get(int index) { return data[Objects.checkIndex(index, size)]; }

    public int set(int index, int value) {
        Objects.checkIndex(index, size);
        int old = data[index];
        data[index] = value;
        return old;
    }

    /** First element; throws {@link NoSuchElementException} if empty. */
    public int first() {
        if (size == 0) throw new NoSuchElementException("empty list");
        return data[0];
    }

    /** Last element; throws {@link NoSuchElementException} if empty. */
    public int last() {
        if (size == 0) throw new NoSuchElementException("empty list");
        return data[size - 1];
    }

    public void ensureCapacity(int minCapacity) {
        if (minCapacity > data.length) data = Arrays.copyOf(data, Growth.arrayLength(data.length, minCapacity));
    }

    /** Releases unused capacity. */
    public void trimToSize() {
        if (size < data.length) data = size == 0 ? EMPTY : Arrays.copyOf(data, size);
    }

    public void add(int value) {
        if (size == data.length) data = Arrays.copyOf(data, Growth.arrayLength(data.length, size + 1));
        data[size++] = value;
        modCount++;
    }

    /** Inserts at {@code index}, shifting later elements right. */
    public void add(int index, int value) {
        Preconditions.position(index, size);
        if (size == data.length) data = Arrays.copyOf(data, Growth.arrayLength(data.length, size + 1));
        System.arraycopy(data, index, data, index + 1, size - index);
        data[index] = value;
        size++;
        modCount++;
    }

    public void addAll(int[] values) {
        Objects.requireNonNull(values, "values");
        addAll(values, 0, values.length);
    }

    public void addAll(int[] values, int off, int len) {
        Objects.requireNonNull(values, "values");
        Preconditions.range(off, len, values.length);
        if (len == 0) return;
        ensureCapacity(Math.addExact(size, len));
        System.arraycopy(values, off, data, size, len);
        size += len;
        modCount++;
    }

    public void addAll(IntList other) {
        Objects.requireNonNull(other, "other");
        addAll(other.data, 0, other.size);
    }

    /** Removes and returns the element at {@code index}, shifting later elements left. */
    public int removeAt(int index) {
        Objects.checkIndex(index, size);
        int old = data[index];
        System.arraycopy(data, index + 1, data, index, size - index - 1);
        size--;
        modCount++;
        return old;
    }

    /** Removes and returns the element at {@code index} by moving the last element into its place; order is not kept. */
    public int swapRemove(int index) {
        Objects.checkIndex(index, size);
        int old = data[index];
        data[index] = data[--size];
        modCount++;
        return old;
    }

    /** Removes and returns the last element; throws {@link NoSuchElementException} if empty. */
    public int pop() {
        if (size == 0) throw new NoSuchElementException("empty list");
        modCount++;
        return data[--size];
    }

    /** Removes the first occurrence of {@code value}; returns whether one was found. */
    public boolean removeValue(int value) {
        int i = indexOf(value);
        if (i < 0) return false;
        removeAt(i);
        return true;
    }

    public int indexOf(int value) {
        int[] d = data;
        for (int i = 0, n = size; i < n; i++) if (d[i] == value) return i;
        return -1;
    }

    public int lastIndexOf(int value) {
        int[] d = data;
        for (int i = size - 1; i >= 0; i--) if (d[i] == value) return i;
        return -1;
    }

    public boolean contains(int value) { return indexOf(value) >= 0; }

    public void clear() {
        size = 0;
        modCount++;
    }

    public void sort() { Arrays.sort(data, 0, size); }

    public void reverse() {
        int[] d = data;
        for (int i = 0, j = size - 1; i < j; i++, j--) { int t = d[i]; d[i] = d[j]; d[j] = t; }
    }

    public int[] toArray() { return Arrays.copyOf(data, size); }

    /** Copies elements {@code [from, to)} into a new array. */
    public int[] toArray(int from, int to) {
        Objects.checkFromToIndex(from, to, size);
        return Arrays.copyOfRange(data, from, to);
    }

    public void forEach(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        int[] d = data;
        for (int i = 0, n = size; i < n; i++) {
            action.accept(d[i]);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
    }

    /** Snapshot stream; later changes to the list are not reflected. */
    public IntStream stream() { return Arrays.stream(data, 0, size); }

    /** Fail-fast iterator; {@code remove()} is not supported. */
    public PrimitiveIterator.OfInt iterator() {
        return new PrimitiveIterator.OfInt() {
            private final int expected = modCount;
            private int next;

            @Override public boolean hasNext() { return next < size; }

            @Override public int nextInt() {
                if (expected != modCount) throw new ConcurrentModificationException();
                if (next >= size) throw new NoSuchElementException();
                return data[next++];
            }
        };
    }

    public IntList copy() {
        IntList out = new IntList(size);
        out.addAll(data, 0, size);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof IntList l) || l.size != size) return false;
        return Arrays.equals(data, 0, size, l.data, 0, size);
    }

    /** Same value as {@code List<Integer>.hashCode()} for equal contents. */
    @Override public int hashCode() {
        int h = 1;
        for (int i = 0; i < size; i++) h = 31 * h + data[i];
        return h;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(data[i]);
        }
        return sb.append(']').toString();
    }
}
