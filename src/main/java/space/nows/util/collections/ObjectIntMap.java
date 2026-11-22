package space.nows.util.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.ObjIntConsumer;

public class ObjectIntMap<K> {
    private final float loadFactor;
    private final int noEntryValue;
    private Object[] keys;
    private int[] values;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;

    public ObjectIntMap() { this(16); }

    public ObjectIntMap(int expectedSize) { this(expectedSize, 0.6f, 0); }

    public ObjectIntMap(int expectedSize, float loadFactor, int noEntryValue) {
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        this.noEntryValue = noEntryValue;
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new Object[cap];
        values = new int[cap];
        mask = cap - 1;
        threshold = HashTables.threshold(cap, loadFactor);
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    private static int hash(Object key) { return HashTables.mix(key.hashCode()); }

    private int find(Object key) {
        int pos = hash(key) & mask;
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

    public int get(K key) { return getOrDefault(key, noEntryValue); }

    public int getOrDefault(K key, int defaultValue) {
        Objects.requireNonNull(key, "key");
        int pos = find(key);
        return pos >= 0 ? values[pos] : defaultValue;
    }

    public int put(K key, int value) {
        Objects.requireNonNull(key, "key");
        int pos = hash(key) & mask;
        Object k;
        while ((k = keys[pos]) != null) {
            if (k == key || k.equals(key)) {
                int old = values[pos];
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

    public int addTo(K key, int delta) {
        Objects.requireNonNull(key, "key");
        int pos = hash(key) & mask;
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

    public int remove(K key) {
        Objects.requireNonNull(key, "key");
        int pos = find(key);
        if (pos < 0) return noEntryValue;
        int old = values[pos];
        modCount++;
        shiftDelete(pos);
        size--;
        return old;
    }

    private void shiftDelete(int pos) {
        Object[] k = keys;
        int[] v = values;
        int last, slot;
        Object curr;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == null) { k[last] = null; return; }
                slot = hash(curr) & mask;
                boolean movable = HashTables.isMovable(last, pos, slot);
                if (movable) break;
                pos = (pos + 1) & mask;
            }
            k[last] = curr;
            v[last] = v[pos];
        }
    }

    private void rehash(int newCap) {
        Object[] ok = keys;
        int[] ov = values;
        keys = new Object[newCap];
        values = new int[newCap];
        mask = newCap - 1;
        threshold = HashTables.threshold(newCap, loadFactor);
        for (int i = 0; i < ok.length; i++) {
            Object k = ok[i];
            if (k != null) {
                int pos = hash(k) & mask;
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
        if (size > 0) Arrays.fill(keys, null);
        modCount++;
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(ObjIntConsumer<? super K> action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        Object[] ks = keys;
        int[] vs = values;
        for (int i = 0; i < ks.length; i++) {
            if (ks[i] != null) {
                action.accept((K) ks[i], vs[i]);
                if (mc != modCount) throw new ConcurrentModificationException();
            }
        }
    }

    /** Stores {@code value} only if {@code key} is absent. Returns the existing value, or the no-entry value if it was absent. */
    public int putIfAbsent(K key, int value) {
        Objects.requireNonNull(key, "key");
        int pos = find(key);
        if (pos >= 0) return values[pos];
        put(key, value);
        return noEntryValue;
    }

    /** Combines the existing value (if any) with {@code value}; stores and returns the result. */
    public int merge(K key, int value, IntBinaryOperator remapping) {
        Objects.requireNonNull(remapping, "remapping");
        Objects.requireNonNull(key, "key");
        int pos = find(key);
        if (pos < 0) {
            put(key, value);
            return value;
        }
        int merged = remapping.applyAsInt(values[pos], value);
        put(key, merged);
        return merged;
    }

    /** Independent copy of the table (keys are shared, not cloned). */
    public ObjectIntMap<K> copy() { return new ObjectIntMap<>(this); }

    private ObjectIntMap(ObjectIntMap<K> o) {
        loadFactor = o.loadFactor;
        noEntryValue = o.noEntryValue;
        keys = o.keys.clone();
        values = o.values.clone();
        mask = o.mask;
        size = o.size;
        threshold = o.threshold;
    }

    /** Boxed snapshot for handing to JDK APIs. */
    public Map<K, Integer> toJavaMap() {
        Map<K, Integer> out = new HashMap<>(HashTables.javaMapCapacity(size()));
        forEach(out::put);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ObjectIntMap<?> m) || m.size() != size()) return false;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                int pos = m.find(keys[i]);
                if (pos < 0 || m.values[pos] != values[i]) return false;
            }
        }
        return true;
    }

    /** Same value as {@code Map<K,Integer>.hashCode()} for equal contents. */
    @Override public int hashCode() {
        int h = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i] != null) h += keys[i].hashCode() ^ values[i];
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
