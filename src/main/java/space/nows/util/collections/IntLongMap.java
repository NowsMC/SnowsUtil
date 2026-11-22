package space.nows.util.collections;

import space.nows.util.function.IntLongConsumer;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongBinaryOperator;

/**
 * Open-addressing hash map from {@code int} keys to {@code long} values, with no per-entry objects.
 * Absent keys read as the no-entry value given at construction ({@code 0L} by default).
 * Not thread-safe; iteration is fail-fast. Generated from {@code HashMap.tpl}; edit the template, not this file.
 */
public class IntLongMap {
    private final float loadFactor;
    private final long noEntryValue;
    private int[] keys;
    private long[] values;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
    private boolean hasZero;
    private long zeroValue;

    public IntLongMap() { this(16); }

    public IntLongMap(int expectedSize) { this(expectedSize, 0.6f, 0L); }

    public IntLongMap(int expectedSize, float loadFactor, long noEntryValue) {
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        this.noEntryValue = noEntryValue;
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new int[cap];
        values = new long[cap];
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

    public long get(int key) { return getOrDefault(key, noEntryValue); }

    public long getOrDefault(int key, long defaultValue) {
        if (key == 0) return hasZero ? zeroValue : defaultValue;
        int pos = find(key);
        return pos >= 0 ? values[pos] : defaultValue;
    }

    public long put(int key, long value) {
        if (key == 0) {
            long old = hasZero ? zeroValue : noEntryValue;
            if (!hasZero) modCount++;
            hasZero = true;
            zeroValue = value;
            return old;
        }
        int pos = HashTables.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) {
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
    public long addTo(int key, long delta) {
        if (key == 0) {
            zeroValue = (hasZero ? zeroValue : 0L) + delta;
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

    public long remove(int key) {
        if (key == 0) {
            if (!hasZero) return noEntryValue;
            modCount++;
            hasZero = false;
            long old = zeroValue;
            zeroValue = 0L;
            return old;
        }
        int pos = find(key);
        if (pos < 0) return noEntryValue;
        long old = values[pos];
        modCount++;
        shiftDelete(pos);
        size--;
        return old;
    }

    private void shiftDelete(int pos) {
        int[] k = keys;
        long[] v = values;
        int last, slot;
        int curr;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == 0) {
                    k[last] = 0;
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
        int[] ok = keys;
        long[] ov = values;
        keys = new int[newCap];
        values = new long[newCap];
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
        }
        modCount++;
        size = 0;
        if (hasZero) modCount++;
        hasZero = false;
        zeroValue = 0L;
    }

    public void forEach(IntLongConsumer action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        if (hasZero) {
            action.accept(0, zeroValue);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
        int[] ks = keys;
        long[] vs = values;
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

    /** Values in the same order as {@link #forEach}. */
    public long[] values() {
        long[] out = new long[size()];
        int n = 0;
        if (hasZero) out[n++] = zeroValue;
        for (int i = 0; i < keys.length; i++) if (keys[i] != 0) out[n++] = values[i];
        return out;
    }

    /** Stores {@code value} only if {@code key} is absent. Returns the existing value, or the no-entry value if it was absent. */
    public long putIfAbsent(int key, long value) {
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
    public long merge(int key, long value, LongBinaryOperator remapping) {
        Objects.requireNonNull(remapping, "remapping");
        long existing;
        if (key == 0) {
            if (!hasZero) {
                put(key, value);
                return value;
            }
            existing = zeroValue;
        } else {
            int pos = find(key);
            if (pos < 0) {
                put(key, value);
                return value;
            }
            existing = values[pos];
        }
        long merged = remapping.applyAsLong(existing, value);
        put(key, merged);
        return merged;
    }

    /** Independent copy of the table. */
    public IntLongMap copy() { return new IntLongMap(this); }

    private IntLongMap(IntLongMap o) {
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
    public Map<Integer, Long> toJavaMap() {
        Map<Integer, Long> out = new HashMap<>(HashTables.javaMapCapacity(size()));
        forEach(out::put);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof IntLongMap m) || m.size() != size()) return false;
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

    /** Same value as {@code Map.hashCode()} of the boxed equivalent for equal contents. */
    @Override public int hashCode() {
        int h = 0;
        if (hasZero) h = Long.hashCode(zeroValue);
        for (int i = 0; i < keys.length; i++) if (keys[i] != 0) h += keys[i] ^ Long.hashCode(values[i]);
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
