package space.nows.util.collections.indexed;

import space.nows.util.error.KeyOutOfRangeException;
import space.nows.util.function.IntObjConsumer;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Map from {@code int} keys in a fixed range {@code [minKey, maxKey]} to values, backed by a plain array.
 * {@link #put} rejects keys outside the range with {@link KeyOutOfRangeException}; {@link #get}, {@link #containsKey}
 * and {@link #remove} treat such keys as absent, like any other map.
 */
public class IntRangeArrayMap<V> {
    private final int minKey;
    private final Object[] values;
    private final boolean[] present;
    private int size;

    public IntRangeArrayMap(int minKey, int maxKey) {
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
            throw new KeyOutOfRangeException(key, minKey, maxKey());
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
        if (size == 0) return;
        Arrays.fill(values, null);
        Arrays.fill(present, false);
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(IntObjConsumer<? super V> action) {
        for (int i = 0; i < values.length; i++) {
            if (present[i]) action.accept(minKey + i, (V) values[i]);
        }
    }

    public int minKey() { return minKey; }

    public int maxKey() { return minKey + (values.length - 1); }

    /** Sorted boxed snapshot for handing to JDK APIs. */
    public Map<Integer, V> toJavaMap() {
        Map<Integer, V> out = new TreeMap<>();
        forEach(out::put);
        return out;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean[] first = {true};
        forEach((k, v) -> {
            if (!first[0]) sb.append(", ");
            first[0] = false;
            sb.append(k).append('=').append(v);
        });
        return sb.append('}').toString();
    }
}
