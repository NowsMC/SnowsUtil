package space.nows.util.collections;

import java.util.Arrays;

public class FixedArrayMap<V> {
    @FunctionalInterface
    public interface Consumer<V> { void accept(int key, V value); }

    private final int minKey;
    private final Object[] values;
    private final boolean[] present;
    private int size;

    public FixedArrayMap(int minKey, int maxKey) {
        if (maxKey < minKey) throw new IllegalArgumentException("maxKey < minKey");
        long len = (long) maxKey - minKey + 1;
        if (len > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("range too large");
        this.minKey = minKey;
        this.values = new Object[(int) len];
        this.present = new boolean[(int) len];
    }

    private int index(int key) {
        int i = key - minKey;
        if (key < minKey || i < 0 || i >= values.length) {
            throw new IndexOutOfBoundsException("key " + key + " outside [" + minKey + ", " + (minKey + values.length - 1) + "]");
        }
        return i;
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    public boolean containsKey(int key) {
        long i = (long) key - minKey;
        return i >= 0 && i < values.length && present[(int) i];
    }

    @SuppressWarnings("unchecked")
    public V get(int key) {
        long i = (long) key - minKey;
        return (i >= 0 && i < values.length) ? (V) values[(int) i] : null;
    }

    @SuppressWarnings("unchecked")
    public V put(int key, V value) {
        int i = index(key);
        V old = (V) values[i];
        values[i] = value;
        if (!present[i]) {
            present[i] = true;
            size++;
        }
        return old;
    }

    @SuppressWarnings("unchecked")
    public V remove(int key) {
        long li = (long) key - minKey;
        if (li < 0 || li >= values.length) return null;
        int i = (int) li;
        V old = (V) values[i];
        if (present[i]) {
            present[i] = false;
            values[i] = null;
            size--;
        }
        return old;
    }

    public void clear() {
        Arrays.fill(values, null);
        Arrays.fill(present, false);
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super V> action) {
        for (int i = 0; i < values.length; i++) {
            if (present[i]) action.accept(minKey + i, (V) values[i]);
        }
    }
}
