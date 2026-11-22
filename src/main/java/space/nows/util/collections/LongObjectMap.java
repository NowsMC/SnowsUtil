package space.nows.util.collections;

import space.nows.util.function.LongObjConsumer;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongFunction;

/**
 * Open-addressing hash map from {@code long} keys to {@code V} values, with no per-entry objects.
 * Not thread-safe; iteration is fail-fast. Generated from {@code HashMap.tpl}; edit the template, not this file.
 */
@SuppressWarnings("unchecked")
public class LongObjectMap<V> {
    private final float loadFactor;
    private long[] keys;
    private Object[] values;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
    private boolean hasZero;
    private V zeroValue;

    public LongObjectMap() { this(16); }

    public LongObjectMap(int expectedSize) { this(expectedSize, 0.6f); }

    public LongObjectMap(int expectedSize, float loadFactor) {
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new long[cap];
        values = new Object[cap];
        mask = cap - 1;
        threshold = HashTables.threshold(cap, loadFactor);
    }

    public int size() { return size + (hasZero ? 1 : 0); }

    public boolean isEmpty() { return size() == 0; }

    private int find(long key) {
        int pos = HashTables.mix(key) & mask;
        long k;
        while ((k = keys[pos]) != 0L) {
            if (k == key) return pos;
            pos = (pos + 1) & mask;
        }
        return -1;
    }

    public boolean containsKey(long key) {
        return key == 0L ? hasZero : find(key) >= 0;
    }

    public V get(long key) { return getOrDefault(key, null); }

    public V getOrDefault(long key, V defaultValue) {
        if (key == 0L) return hasZero ? zeroValue : defaultValue;
        int pos = find(key);
        return pos >= 0 ? (V) values[pos] : defaultValue;
    }

    public V put(long key, V value) {
        if (key == 0L) {
            V old = hasZero ? zeroValue : null;
            if (!hasZero) modCount++;
            hasZero = true;
            zeroValue = value;
            return old;
        }
        int pos = HashTables.mix(key) & mask;
        long k;
        while ((k = keys[pos]) != 0L) {
            if (k == key) {
                V old = (V) values[pos];
                values[pos] = value;
                return old;
            }
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        values[pos] = value;
        if (++size >= threshold) rehash(HashTables.grow(keys.length));
        return null;
    }

    public V remove(long key) {
        if (key == 0L) {
            if (!hasZero) return null;
            modCount++;
            hasZero = false;
            V old = zeroValue;
            zeroValue = null;
            return old;
        }
        int pos = find(key);
        if (pos < 0) return null;
        V old = (V) values[pos];
        modCount++;
        shiftDelete(pos);
        size--;
        return old;
    }

    private void shiftDelete(int pos) {
        long[] k = keys;
        Object[] v = values;
        int last, slot;
        long curr;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == 0L) {
                    k[last] = 0L;
                    v[last] = null;
                    return;
                }
                slot = HashTables.mix(curr) & mask;
                if (HashTables.isMovable(last, pos, slot)) break;
                pos = (pos + 1) & mask;
            }
            k[last] = curr;
            v[last] = v[pos];
        }
    }

    private void rehash(int newCap) {
        long[] ok = keys;
        Object[] ov = values;
        keys = new long[newCap];
        values = new Object[newCap];
        mask = newCap - 1;
        threshold = HashTables.threshold(newCap, loadFactor);
        for (int i = 0; i < ok.length; i++) {
            long k = ok[i];
            if (k != 0L) {
                int pos = HashTables.mix(k) & mask;
                while (keys[pos] != 0L) pos = (pos + 1) & mask;
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
            Arrays.fill(keys, 0L);
            Arrays.fill(values, null);
        }
        modCount++;
        size = 0;
        if (hasZero) modCount++;
        hasZero = false;
        zeroValue = null;
    }

    public void forEach(LongObjConsumer<? super V> action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        if (hasZero) {
            action.accept(0L, zeroValue);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
        long[] ks = keys;
        Object[] vs = values;
        for (int i = 0; i < ks.length; i++) {
            long k = ks[i];
            if (k != 0L) {
                action.accept(k, (V) vs[i]);
                if (mc != modCount) throw new ConcurrentModificationException();
            }
        }
    }

    public long[] keys() {
        long[] out = new long[size()];
        int n = 0;
        if (hasZero) out[n++] = 0L;
        for (long k : keys) if (k != 0L) out[n++] = k;
        return out;
    }

    /** Stores {@code value} only if {@code key} is absent. Returns the existing value, or {@code null} if it was absent. */
    public V putIfAbsent(long key, V value) {
        if (key == 0L) {
            if (hasZero) return zeroValue;
        } else {
            int pos = find(key);
            if (pos >= 0) return (V) values[pos];
        }
        put(key, value);
        return null;
    }

    /** Returns the value for {@code key}, computing and storing it first if absent. */
    public V computeIfAbsent(long key, LongFunction<? extends V> mapping) {
        Objects.requireNonNull(mapping, "mapping");
        if (key == 0L) {
            if (hasZero) return zeroValue;
        } else {
            int pos = find(key);
            if (pos >= 0) return (V) values[pos];
        }
        V created = mapping.apply(key);
        put(key, created);
        return created;
    }

    /** Independent copy of the table (values are shared, not cloned). */
    public LongObjectMap<V> copy() { return new LongObjectMap<>(this); }

    private LongObjectMap(LongObjectMap<V> o) {
        loadFactor = o.loadFactor;
        keys = o.keys.clone();
        values = o.values.clone();
        mask = o.mask;
        size = o.size;
        threshold = o.threshold;
        hasZero = o.hasZero;
        zeroValue = o.zeroValue;
    }

    /** Boxed snapshot for handing to JDK APIs. */
    public Map<Long, V> toJavaMap() {
        Map<Long, V> out = new HashMap<>(HashTables.javaMapCapacity(size()));
        forEach(out::put);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LongObjectMap<?> m) || m.size() != size()) return false;
        if (hasZero && (!m.hasZero || !Objects.equals(m.zeroValue, zeroValue))) return false;
        for (int i = 0; i < keys.length; i++) {
            long k = keys[i];
            if (k != 0L) {
                int pos = m.find(k);
                if (pos < 0 || !Objects.equals(m.values[pos], values[i])) return false;
            }
        }
        return true;
    }

    /** Same value as {@code Map.hashCode()} of the boxed equivalent for equal contents. */
    @Override public int hashCode() {
        int h = 0;
        if (hasZero) h = Objects.hashCode(zeroValue);
        for (int i = 0; i < keys.length; i++) if (keys[i] != 0L) h += Long.hashCode(keys[i]) ^ Objects.hashCode(values[i]);
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
