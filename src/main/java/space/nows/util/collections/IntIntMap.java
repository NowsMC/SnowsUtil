package space.nows.util.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import space.nows.util.function.IntIntConsumer;

public class IntIntMap {
    private final float loadFactor;
    private final int noEntryValue;
    private int[] keys;
    private int[] values;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
    private boolean hasZero;
    private int zeroValue;

    public IntIntMap() { this(16); }

    public IntIntMap(int expectedSize) { this(expectedSize, 0.6f, 0); }

    public IntIntMap(int expectedSize, float loadFactor, int noEntryValue) {
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        this.noEntryValue = noEntryValue;
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new int[cap];
        values = new int[cap];
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

    public int get(int key) { return getOrDefault(key, noEntryValue); }

    public int getOrDefault(int key, int defaultValue) {
        if (key == 0) return hasZero ? zeroValue : defaultValue;
        int pos = find(key);
        return pos >= 0 ? values[pos] : defaultValue;
    }

    public int put(int key, int value) {
        if (key == 0) {
            int old = hasZero ? zeroValue : noEntryValue;
            if (!hasZero) modCount++;
            hasZero = true;
            zeroValue = value;
            return old;
        }
        int pos = HashTables.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) {
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

    public int addTo(int key, int delta) {
        if (key == 0) {
            zeroValue = (hasZero ? zeroValue : 0) + delta;
            if (!hasZero) modCount++;
            hasZero = true;
            return zeroValue;
        }
        int pos = HashTables.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) return values[pos] += delta;
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        values[pos] = delta;
        if (++size >= threshold) rehash(HashTables.grow(keys.length));
        return delta;
    }

    public int remove(int key) {
        if (key == 0) {
            if (!hasZero) return noEntryValue;
            if (hasZero) modCount++;
            hasZero = false;
            int old = zeroValue;
            zeroValue = 0;
            return old;
        }
        int pos = find(key);
        if (pos < 0) return noEntryValue;
        int old = values[pos];
        modCount++;
        shiftDelete(pos);
        size--;
        return old;
    }

    private void shiftDelete(int pos) {
        int[] k = keys, v = values;
        int last, curr, slot;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == 0) { k[last] = 0; return; }
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
        int[] ok = keys, ov = values;
        keys = new int[newCap];
        values = new int[newCap];
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
        if (size > 0) Arrays.fill(keys, 0);
        modCount++;
        size = 0;
        if (hasZero) modCount++;
        hasZero = false;
        zeroValue = 0;
    }

    public void forEach(IntIntConsumer action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        if (hasZero) {
            action.accept(0, zeroValue);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
        int[] ks = keys, vs = values;
        for (int i = 0; i < ks.length; i++) {
            int k = ks[i];
            if (k != 0) {
                action.accept(k, vs[i]);
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

    /** Values in the same order as {@link #keys()}. */
    public int[] values() {
        int[] out = new int[size()];
        int n = 0;
        if (hasZero) out[n++] = zeroValue;
        for (int i = 0; i < keys.length; i++) if (keys[i] != 0) out[n++] = values[i];
        return out;
    }

    /** Stores {@code value} only if {@code key} is absent. Returns the existing value, or the no-entry value if it was absent. */
    public int putIfAbsent(int key, int value) {
        if (key == 0) {
            if (hasZero) return zeroValue;
        } else {
            int pos = find(key);
            if (pos >= 0) return values[pos];
        }
        put(key, value);
        return noEntryValue;
    }

    /** Combines the existing value (if any) with {@code value}; stores and returns the result. */
    public int merge(int key, int value, IntBinaryOperator remapping) {
        Objects.requireNonNull(remapping, "remapping");
        if (key == 0) {
            if (!hasZero) {
                put(key, value);
                return value;
            }
            int merged = remapping.applyAsInt(zeroValue, value);
            put(key, merged);
            return merged;
        }
        int pos = find(key);
        if (pos < 0) {
            put(key, value);
            return value;
        }
        int merged = remapping.applyAsInt(values[pos], value);
        put(key, merged);
        return merged;
    }

    /** Independent deep copy. */
    public IntIntMap copy() { return new IntIntMap(this); }

    private IntIntMap(IntIntMap o) {
        loadFactor = o.loadFactor;
        noEntryValue = o.noEntryValue;
        keys = o.keys.clone();
        values = o.values.clone();
        mask = o.mask;
        size = o.size;
        threshold = o.threshold;
        hasZero = o.hasZero;
        zeroValue = o.zeroValue;
    }

    /** Boxed snapshot for handing to JDK APIs. */
    public Map<Integer, Integer> toJavaMap() {
        Map<Integer, Integer> out = new HashMap<>(HashTables.javaMapCapacity(size()));
        forEach(out::put);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof IntIntMap m) || m.size() != size()) return false;
        if (hasZero && (!m.hasZero || m.zeroValue != zeroValue)) return false;
        for (int i = 0; i < keys.length; i++) {
            int k = keys[i];
            if (k != 0) {
                int pos = m.find(k);
                if (pos < 0 || m.values[pos] != values[i]) return false;
            }
        }
        return true;
    }

    /** Same value as {@code Map<Integer,Integer>.hashCode()} for equal contents. */
    @Override public int hashCode() {
        int h = hasZero ? zeroValue : 0;
        for (int i = 0; i < keys.length; i++) if (keys[i] != 0) h += keys[i] ^ values[i];
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
