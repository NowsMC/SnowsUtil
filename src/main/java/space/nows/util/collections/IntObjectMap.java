package space.nows.util.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import space.nows.util.function.IntObjConsumer;

public class IntObjectMap<V> {
    private final float loadFactor;
    private int[] keys;
    private Object[] values;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
    private boolean hasZero;
    private V zeroValue;

    public IntObjectMap() { this(16); }

    public IntObjectMap(int expectedSize) { this(expectedSize, 0.6f); }

    public IntObjectMap(int expectedSize, float loadFactor) {
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new int[cap];
        values = new Object[cap];
        mask = cap - 1;
        threshold = HashTables.threshold(cap, loadFactor);
    }

    public int size() { return size + (hasZero ? 1 : 0); }

    public boolean isEmpty() { return size() == 0; }

    private int find(int key) {
        int pos = HashTables.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) return pos;
            pos = (pos + 1) & mask;
        }
        return -1;
    }

    public boolean containsKey(int key) {
        return key == 0 ? hasZero : find(key) >= 0;
    }

    @SuppressWarnings("unchecked")
    public V get(int key) {
        if (key == 0) return hasZero ? zeroValue : null;
        int pos = find(key);
        return pos >= 0 ? (V) values[pos] : null;
    }

    @SuppressWarnings("unchecked")
    public V getOrDefault(int key, V defaultValue) {
        if (key == 0) return hasZero ? zeroValue : defaultValue;
        int pos = find(key);
        return pos >= 0 ? (V) values[pos] : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public V put(int key, V value) {
        if (key == 0) {
            V old = hasZero ? zeroValue : null;
            if (!hasZero) modCount++;
            hasZero = true;
            zeroValue = value;
            return old;
        }
        int pos = HashTables.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
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

    @SuppressWarnings("unchecked")
    public V remove(int key) {
        if (key == 0) {
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
        int[] k = keys;
        Object[] v = values;
        int last, curr, slot;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == 0) { k[last] = 0; v[last] = null; return; }
                slot = HashTables.mix(curr) & mask;
                boolean movable = HashTables.isMovable(last, pos, slot);
                if (movable) break;
                pos = (pos + 1) & mask;
            }
            k[last] = curr;
            v[last] = v[pos];
        }
    }

    private void rehash(int newCap) {
        int[] ok = keys;
        Object[] ov = values;
        keys = new int[newCap];
        values = new Object[newCap];
        mask = newCap - 1;
        threshold = HashTables.threshold(newCap, loadFactor);
        for (int i = 0; i < ok.length; i++) {
            int k = ok[i];
            if (k != 0) {
                int pos = HashTables.mix(k) & mask;
                while (keys[pos] != 0) pos = (pos + 1) & mask;
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
            Arrays.fill(keys, 0);
            Arrays.fill(values, null);
        }
        modCount++;
        size = 0;
        if (hasZero) modCount++;
        hasZero = false;
        zeroValue = null;
    }

    @SuppressWarnings("unchecked")
    public void forEach(IntObjConsumer<? super V> action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        if (hasZero) {
            action.accept(0, zeroValue);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
        int[] ks = keys;
        Object[] vs = values;
        for (int i = 0; i < ks.length; i++) {
            if (ks[i] != 0) {
                action.accept(ks[i], (V) vs[i]);
                if (mc != modCount) throw new ConcurrentModificationException();
            }
        }
    }

    public int[] keys() {
        int[] out = new int[size()];
        int n = 0;
        if (hasZero) out[n++] = 0;
        for (int k : keys) if (k != 0) out[n++] = k;
        return out;
    }

    /** Stores {@code value} only if {@code key} is absent. Returns the existing value, or {@code null} if it was absent. */
    @SuppressWarnings("unchecked")
    public V putIfAbsent(int key, V value) {
        if (key == 0) {
            if (hasZero) return zeroValue;
        } else {
            int pos = find(key);
            if (pos >= 0) return (V) values[pos];
        }
        put(key, value);
        return null;
    }

    /** Returns the value for {@code key}, computing and storing it first if absent. */
    @SuppressWarnings("unchecked")
    public V computeIfAbsent(int key, IntFunction<? extends V> mapping) {
        Objects.requireNonNull(mapping, "mapping");
        if (key == 0) {
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
    public IntObjectMap<V> copy() { return new IntObjectMap<>(this); }

    private IntObjectMap(IntObjectMap<V> o) {
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
    public Map<Integer, V> toJavaMap() {
        Map<Integer, V> out = new HashMap<>(HashTables.javaMapCapacity(size()));
        forEach(out::put);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof IntObjectMap<?> m) || m.size() != size()) return false;
        if (hasZero && (!m.hasZero || !Objects.equals(zeroValue, m.zeroValue))) return false;
        for (int i = 0; i < keys.length; i++) {
            int k = keys[i];
            if (k != 0) {
                int pos = m.find(k);
                if (pos < 0 || !Objects.equals(values[i], m.values[pos])) return false;
            }
        }
        return true;
    }

    /** Same value as {@code Map<K,V>.hashCode()} for equal contents. */
    @Override public int hashCode() {
        int h = hasZero ? 0 ^ Objects.hashCode(zeroValue) : 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != 0) h += keys[i] ^ Objects.hashCode(values[i]);
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
