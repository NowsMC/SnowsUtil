package space.nows.util.collections;

import space.nows.util.check.Preconditions;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.BiConsumer;

/**
 * Map that stores entries in insertion-ordered parallel arrays and searches them linearly. Lookups are O(n),
 * so use it only for a handful of entries; it avoids the per-entry overhead of a hash table.
 */
public class SmallArrayMap<K, V> {
    private Object[] keys;
    private Object[] values;
    private int size;

    public SmallArrayMap() { this(4); }

    public SmallArrayMap(int initialCapacity) {
        int cap = Math.max(1, Preconditions.nonNegative(initialCapacity, "initialCapacity"));
        keys = new Object[cap];
        values = new Object[cap];
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    private int indexOf(Object key) {
        Object[] ks = keys;
        for (int i = 0; i < size; i++) {
            Object k = ks[i];
            if (k == key || (key != null && key.equals(k))) return i;
        }
        return -1;
    }

    public boolean containsKey(Object key) { return indexOf(key) >= 0; }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        int i = indexOf(key);
        return i >= 0 ? (V) values[i] : null;
    }

    @SuppressWarnings("unchecked")
    public V getOrDefault(Object key, V defaultValue) {
        int i = indexOf(key);
        return i >= 0 ? (V) values[i] : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        int i = indexOf(key);
        if (i >= 0) {
            V old = (V) values[i];
            values[i] = value;
            return old;
        }
        if (size == keys.length) {
            int newCap = size + (size >> 1) + 1;
            keys = Arrays.copyOf(keys, newCap);
            values = Arrays.copyOf(values, newCap);
        }
        keys[size] = key;
        values[size] = value;
        size++;
        return null;
    }

    /** Removes by moving the last entry into the vacated slot, so the iteration order changes. */
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        int i = indexOf(key);
        if (i < 0) return null;
        V old = (V) values[i];
        int last = --size;
        keys[i] = keys[last];
        values[i] = values[last];
        keys[last] = null;
        values[last] = null;
        return old;
    }

    public void clear() {
        Arrays.fill(keys, 0, size, null);
        Arrays.fill(values, 0, size, null);
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super K, ? super V> action) {
        for (int i = 0; i < size; i++) action.accept((K) keys[i], (V) values[i]);
    }

    /** Stores {@code value} only if {@code key} is absent. Returns the existing value, or {@code null} if it was absent. */
    public V putIfAbsent(K key, V value) {
        if (containsKey(key)) return get(key);
        put(key, value);
        return null;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mapping) {
        Objects.requireNonNull(mapping, "mapping");
        if (containsKey(key)) return get(key);
        V created = mapping.apply(key);
        put(key, created);
        return created;
    }

    public SmallArrayMap<K, V> copy() {
        SmallArrayMap<K, V> out = new SmallArrayMap<>(Math.max(1, size));
        forEach(out::put);
        return out;
    }

    /** Snapshot in this map's current (arbitrary) order. */
    public Map<K, V> toJavaMap() {
        Map<K, V> out = new LinkedHashMap<>();
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
