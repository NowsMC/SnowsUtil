package space.nows.util.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongFunction;
import space.nows.util.function.LongObjConsumer;

public class LongHashMap<V> {
    private final float loadFactor;
    private long[] keys;
    private Object[] values;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
    private boolean hasZero;
    private V zeroValue;

    public LongHashMap() { this(16); }

    public LongHashMap(int expectedSize) { this(expectedSize, 0.6f); }

    public LongHashMap(int expectedSize, float loadFactor) {
        Hash.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        int cap = Hash.capacityFor(expectedSize, loadFactor);
        keys = new long[cap];
        values = new Object[cap];
        mask = cap - 1;
        threshold = Hash.threshold(cap, loadFactor);
    }

    public int size() { return size + (hasZero ? 1 : 0); }

    public boolean isEmpty() { return size() == 0; }

    private int find(long key) {
        int pos = Hash.mix(key) & mask;
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

    @SuppressWarnings("unchecked")
    public V get(long key) {
        if (key == 0L) return hasZero ? zeroValue : null;
        int pos = find(key);
        return pos >= 0 ? (V) values[pos] : null;
    }

    @SuppressWarnings("unchecked")
    public V put(long key, V value) {
        if (key == 0L) {
            V old = hasZero ? zeroValue : null;
            if (!hasZero) modCount++;
            hasZero = true;
            zeroValue = value;
            return old;
        }
        int pos = Hash.mix(key) & mask;
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
        if (++size >= threshold) rehash(Hash.grow(keys.length));
        return null;
    }

    @SuppressWarnings("unchecked")
    public V remove(long key) {
        if (key == 0L) {
            if (!hasZero) return null;
            V old = zeroValue;
            if (hasZero) modCount++;
            hasZero = false;
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
                if (curr == 0L) { k[last] = 0L; v[last] = null; return; }
                slot = Hash.mix(curr) & mask;
                boolean movable = Hash.isMovable(last, pos, slot);
                if (movable) break;
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
        threshold = Hash.threshold(newCap, loadFactor);
        for (int i = 0; i < ok.length; i++) {
            long k = ok[i];
            if (k != 0L) {
                int pos = Hash.mix(k) & mask;
                while (keys[pos] != 0L) pos = (pos + 1) & mask;
                keys[pos] = k;
                values[pos] = ov[i];
            }
        }
    }

    public void clear() {
        Arrays.fill(keys, 0L);
        Arrays.fill(values, null);
        modCount++;
        size = 0;
        if (hasZero) modCount++;
        hasZero = false;
        zeroValue = null;
    }

    @SuppressWarnings("unchecked")
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
            if (ks[i] != 0L) {
                action.accept(ks[i], (V) vs[i]);
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
        if (containsKey(key)) return get(key);
        put(key, value);
        return null;
    }

    /** Returns the value for {@code key}, computing and storing it first if absent. */
    public V computeIfAbsent(long key, LongFunction<? extends V> mapping) {
        Objects.requireNonNull(mapping, "mapping");
        if (containsKey(key)) return get(key);
        V created = mapping.apply(key);
        put(key, created);
        return created;
    }

    /** Independent copy of the table (values are shared, not cloned). */
    public LongHashMap<V> copy() { return new LongHashMap<>(this); }

    private LongHashMap(LongHashMap<V> o) {
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
        Map<Long, V> out = new HashMap<>(Hash.javaMapCapacity(size()));
        forEach(out::put);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LongHashMap<?> m) || m.size() != size()) return false;
        if (hasZero && (!m.hasZero || !Objects.equals(zeroValue, m.zeroValue))) return false;
        for (int i = 0; i < keys.length; i++) {
            long k = keys[i];
            if (k != 0L) {
                int pos = m.find(k);
                if (pos < 0 || !Objects.equals(values[i], m.values[pos])) return false;
            }
        }
        return true;
    }

    /** Same value as {@code Map<K,V>.hashCode()} for equal contents. */
    @Override public int hashCode() {
        int h = hasZero ? Long.hashCode(0L) ^ Objects.hashCode(zeroValue) : 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != 0L) h += Long.hashCode(keys[i]) ^ Objects.hashCode(values[i]);
        }
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
