package space.nows.util.collections;

import space.nows.util.function.LongLongConsumer;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongBinaryOperator;

public class LongLongMap {
    private final float loadFactor;
    private final long noEntryValue;
    private long[] keys;
    private long[] values;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
    private boolean hasZero;
    private long zeroValue;

    public LongLongMap() { this(16); }

    public LongLongMap(int expectedSize) { this(expectedSize, 0.6f, 0L); }

    public LongLongMap(int expectedSize, float loadFactor, long noEntryValue) {
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        this.noEntryValue = noEntryValue;
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new long[cap];
        values = new long[cap];
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

    public long get(long key) { return getOrDefault(key, noEntryValue); }

    public long getOrDefault(long key, long defaultValue) {
        if (key == 0L) return hasZero ? zeroValue : defaultValue;
        int pos = find(key);
        return pos >= 0 ? values[pos] : defaultValue;
    }

    public long put(long key, long value) {
        if (key == 0L) {
            long old = hasZero ? zeroValue : noEntryValue;
            if (!hasZero) modCount++;
            hasZero = true;
            zeroValue = value;
            return old;
        }
        int pos = HashTables.mix(key) & mask;
        long k;
        while ((k = keys[pos]) != 0L) {
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

    public long addTo(long key, long delta) {
        if (key == 0L) {
            zeroValue = (hasZero ? zeroValue : 0L) + delta;
            if (!hasZero) modCount++;
            hasZero = true;
            return zeroValue;
        }
        int pos = HashTables.mix(key) & mask;
        long k;
        while ((k = keys[pos]) != 0L) {
            if (k == key) return values[pos] += delta;
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        values[pos] = delta;
        if (++size >= threshold) rehash(HashTables.grow(keys.length));
        return delta;
    }

    public long remove(long key) {
        if (key == 0L) {
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
        long[] k = keys, v = values;
        int last, slot;
        long curr;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == 0L) { k[last] = 0L; return; }
                slot = HashTables.mix(curr) & mask;
                if (HashTables.isMovable(last, pos, slot)) break;
                pos = (pos + 1) & mask;
            }
            k[last] = curr;
            v[last] = v[pos];
        }
    }

    private void rehash(int newCap) {
        long[] ok = keys, ov = values;
        keys = new long[newCap];
        values = new long[newCap];
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
        if (size > 0) Arrays.fill(keys, 0L);
        modCount++;
        size = 0;
        if (hasZero) modCount++;
        hasZero = false;
        zeroValue = 0L;
    }

    public void forEach(LongLongConsumer action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        if (hasZero) {
            action.accept(0L, zeroValue);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
        long[] ks = keys, vs = values;
        for (int i = 0; i < ks.length; i++) {
            long k = ks[i];
            if (k != 0L) {
                action.accept(k, vs[i]);
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

    /** Values in the same order as {@link #keys()}. */
    public long[] values() {
        long[] out = new long[size()];
        int n = 0;
        if (hasZero) out[n++] = zeroValue;
        for (int i = 0; i < keys.length; i++) if (keys[i] != 0L) out[n++] = values[i];
        return out;
    }

    /** Stores {@code value} only if {@code key} is absent. Returns the existing value, or the no-entry value if it was absent. */
    public long putIfAbsent(long key, long value) {
        if (key == 0L) {
            if (hasZero) return zeroValue;
        } else {
            int pos = find(key);
            if (pos >= 0) return values[pos];
        }
        put(key, value);
        return noEntryValue;
    }

    /** Combines the existing value (if any) with {@code value}; stores and returns the result. */
    public long merge(long key, long value, LongBinaryOperator remapping) {
        Objects.requireNonNull(remapping, "remapping");
        long existing;
        if (key == 0L) {
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

    /** Independent deep copy. */
    public LongLongMap copy() { return new LongLongMap(this); }

    private LongLongMap(LongLongMap o) {
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
    public Map<Long, Long> toJavaMap() {
        Map<Long, Long> out = new HashMap<>(HashTables.javaMapCapacity(size()));
        forEach(out::put);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LongLongMap m) || m.size() != size()) return false;
        if (hasZero && (!m.hasZero || m.zeroValue != zeroValue)) return false;
        for (int i = 0; i < keys.length; i++) {
            long k = keys[i];
            if (k != 0L) {
                int pos = m.find(k);
                if (pos < 0 || m.values[pos] != values[i]) return false;
            }
        }
        return true;
    }

    /** Same value as {@code Map<Long,Long>.hashCode()} for equal contents. */
    @Override public int hashCode() {
        int h = hasZero ? Long.hashCode(zeroValue) : 0;
        for (int i = 0; i < keys.length; i++) if (keys[i] != 0L) h += Long.hashCode(keys[i]) ^ Long.hashCode(values[i]);
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
