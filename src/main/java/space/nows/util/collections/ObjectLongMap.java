package space.nows.util.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongBinaryOperator;
import java.util.function.ObjLongConsumer;

/**
 * Open-addressing hash map from {@code K} keys to {@code long} values, with no per-entry objects.
 * Absent keys read as the no-entry value given at construction ({@code 0L} by default).
 * Not thread-safe; iteration is fail-fast. Generated from {@code HashMap.tpl}; edit the template, not this file.
 */
@SuppressWarnings("unchecked")
public class ObjectLongMap<K> {
    private final float loadFactor;
    private final long noEntryValue;
    private Object[] keys;
    private long[] values;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;

    public ObjectLongMap() { this(16); }

    public ObjectLongMap(int expectedSize) { this(expectedSize, 0.6f, 0L); }

    public ObjectLongMap(int expectedSize, float loadFactor, long noEntryValue) {
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        this.noEntryValue = noEntryValue;
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new Object[cap];
        values = new long[cap];
        mask = cap - 1;
        threshold = HashTables.threshold(cap, loadFactor);
    }

    public int size() { return size; }

    public boolean isEmpty() { return size() == 0; }

    private int find(Object key) {
        int pos = HashTables.mix(key.hashCode()) & mask;
        Object k;
        while ((k = keys[pos]) != null) {
            if (k == key || k.equals(key)) return pos;
            pos = (pos + 1) & mask;
        }
        return -1;
    }

    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "key");
        return find(key) >= 0;
    }

    public long get(K key) { return getOrDefault(key, noEntryValue); }

    public long getOrDefault(K key, long defaultValue) {
        Objects.requireNonNull(key, "key");
        int pos = find(key);
        return pos >= 0 ? values[pos] : defaultValue;
    }

    public long put(K key, long value) {
        Objects.requireNonNull(key, "key");
        int pos = HashTables.mix(key.hashCode()) & mask;
        Object k;
        while ((k = keys[pos]) != null) {
            if (k == key || k.equals(key)) {
                long old = values[pos];
                values[pos] = value;
                return old;
            }
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        values[pos] = value;
        if (++size >= threshold) rehash(HashTables.grow(keys.length));
        return noEntryValue;
    }

    /** Adds {@code delta} to the value for {@code key}, treating an absent key as 0; returns the new value. */
    public long addTo(K key, long delta) {
        Objects.requireNonNull(key, "key");
        int pos = HashTables.mix(key.hashCode()) & mask;
        Object k;
        while ((k = keys[pos]) != null) {
            if (k == key || k.equals(key)) return values[pos] += delta;
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        values[pos] = delta;
        if (++size >= threshold) rehash(HashTables.grow(keys.length));
        return delta;
    }

    public long remove(K key) {
        Objects.requireNonNull(key, "key");
        int pos = find(key);
        if (pos < 0) return noEntryValue;
        long old = values[pos];
        modCount++;
        shiftDelete(pos);
        size--;
        return old;
    }

    private void shiftDelete(int pos) {
        Object[] k = keys;
        long[] v = values;
        int last, slot;
        Object curr;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == null) {
                    k[last] = null;
                    return;
                }
                slot = HashTables.mix(curr.hashCode()) & mask;
                if (HashTables.isMovable(last, pos, slot)) break;
                pos = (pos + 1) & mask;
            }
            k[last] = curr;
            v[last] = v[pos];
        }
    }

    private void rehash(int newCap) {
        Object[] ok = keys;
        long[] ov = values;
        keys = new Object[newCap];
        values = new long[newCap];
        mask = newCap - 1;
        threshold = HashTables.threshold(newCap, loadFactor);
        for (int i = 0; i < ok.length; i++) {
            Object k = ok[i];
            if (k != null) {
                int pos = HashTables.mix(k.hashCode()) & mask;
                while (keys[pos] != null) pos = (pos + 1) & mask;
                keys[pos] = k;
                values[pos] = ov[i];
            }
        }
    }

    /** Shrinks the table to the smallest size that holds the current entries. */
    public void trim() {
        int cap = HashTables.capacityFor(size, loadFactor);
        if (cap < keys.length) {
            modCount++;
            rehash(cap);
        }
    }

    public void clear() {
        if (size > 0) {
            Arrays.fill(keys, null);
        }
        modCount++;
        size = 0;
    }

    public void forEach(ObjLongConsumer<? super K> action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        Object[] ks = keys;
        long[] vs = values;
        for (int i = 0; i < ks.length; i++) {
            Object k = ks[i];
            if (k != null) {
                action.accept((K) k, vs[i]);
                if (mc != modCount) throw new ConcurrentModificationException();
            }
        }
    }

    /** Values in the same order as {@link #forEach}. */
    public long[] values() {
        long[] out = new long[size()];
        int n = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i] != null) out[n++] = values[i];
        return out;
    }

    /** Stores {@code value} only if {@code key} is absent. Returns the existing value, or the no-entry value if it was absent. */
    public long putIfAbsent(K key, long value) {
        Objects.requireNonNull(key, "key");
        int pos = find(key);
        if (pos >= 0) return values[pos];
        put(key, value);
        return noEntryValue;
    }

    /** Combines the existing value (if any) with {@code value}; stores and returns the result. */
    public long merge(K key, long value, LongBinaryOperator remapping) {
        Objects.requireNonNull(remapping, "remapping");
        Objects.requireNonNull(key, "key");
        long existing;
        int pos = find(key);
        if (pos < 0) {
            put(key, value);
            return value;
        }
        existing = values[pos];
        long merged = remapping.applyAsLong(existing, value);
        put(key, merged);
        return merged;
    }

    /** Independent copy of the table (keys are shared, not cloned). */
    public ObjectLongMap<K> copy() { return new ObjectLongMap<>(this); }

    private ObjectLongMap(ObjectLongMap<K> o) {
        loadFactor = o.loadFactor;
        noEntryValue = o.noEntryValue;
        keys = o.keys.clone();
        values = o.values.clone();
        mask = o.mask;
        size = o.size;
        threshold = o.threshold;
    }

    /** Boxed snapshot for handing to JDK APIs. */
    public Map<K, Long> toJavaMap() {
        Map<K, Long> out = new HashMap<>(HashTables.javaMapCapacity(size()));
        forEach(out::put);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ObjectLongMap<?> m) || m.size() != size()) return false;
        for (int i = 0; i < keys.length; i++) {
            Object k = keys[i];
            if (k != null) {
                int pos = m.find(k);
                if (pos < 0 || m.values[pos] != values[i]) return false;
            }
        }
        return true;
    }

    /** Same value as {@code Map.hashCode()} of the boxed equivalent for equal contents. */
    @Override public int hashCode() {
        int h = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i] != null) h += keys[i].hashCode() ^ Long.hashCode(values[i]);
        return h;
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
