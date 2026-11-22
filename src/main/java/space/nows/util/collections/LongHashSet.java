package space.nows.util.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;

/**
 * Open-addressing hash set of {@code long} values, with no per-element objects. Not thread-safe; iteration is
 * fail-fast. Generated from {@code HashSet.tpl}; edit the template, not this file.
 */
public class LongHashSet {
    private final float loadFactor;
    private long[] keys;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
    private boolean hasZero;

    public LongHashSet() { this(16); }

    public LongHashSet(int expectedSize) { this(expectedSize, 0.6f); }

    public LongHashSet(int expectedSize, float loadFactor) {
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new long[cap];
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

    public boolean contains(long key) {
        return key == 0L ? hasZero : find(key) >= 0;
    }

    public boolean add(long key) {
        if (key == 0L) {
            boolean added = !hasZero;
            if (added) modCount++;
            hasZero = true;
            return added;
        }
        int pos = HashTables.mix(key) & mask;
        long k;
        while ((k = keys[pos]) != 0L) {
            if (k == key) return false;
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        if (++size >= threshold) rehash(HashTables.grow(keys.length));
        return true;
    }

    public boolean remove(long key) {
        if (key == 0L) {
            boolean had = hasZero;
            if (had) modCount++;
            hasZero = false;
            return had;
        }
        int pos = find(key);
        if (pos < 0) return false;
        modCount++;
        shiftDelete(pos);
        size--;
        return true;
    }

    private void shiftDelete(int pos) {
        long[] k = keys;
        int last, slot;
        long curr;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == 0L) {
                    k[last] = 0L;
                    return;
                }
                slot = HashTables.mix(curr) & mask;
                if (HashTables.isMovable(last, pos, slot)) break;
                pos = (pos + 1) & mask;
            }
            k[last] = curr;
        }
    }

    private void rehash(int newCap) {
        long[] ok = keys;
        keys = new long[newCap];
        mask = newCap - 1;
        threshold = HashTables.threshold(newCap, loadFactor);
        for (long k : ok) {
            if (k != 0L) {
                int pos = HashTables.mix(k) & mask;
                while (keys[pos] != 0L) pos = (pos + 1) & mask;
                keys[pos] = k;
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
    }

    public void forEach(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        if (hasZero) {
            action.accept(0L);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
        long[] ks = keys;
        for (int i = 0; i < ks.length; i++) {
            if (ks[i] != 0L) {
                action.accept(ks[i]);
                if (mc != modCount) throw new ConcurrentModificationException();
            }
        }
    }

    public long[] toArray() {
        long[] out = new long[size()];
        int n = 0;
        if (hasZero) out[n++] = 0L;
        for (long k : keys) if (k != 0L) out[n++] = k;
        return out;
    }

    public static LongHashSet of(long... values) {
        Objects.requireNonNull(values, "values");
        LongHashSet set = new LongHashSet(values.length);
        set.addAll(values);
        return set;
    }

    /** Adds every value; returns {@code true} if the set changed. */
    public boolean addAll(long[] values) {
        Objects.requireNonNull(values, "values");
        boolean changed = false;
        for (long v : values) changed |= add(v);
        return changed;
    }

    /** Adds every element of {@code other}; returns {@code true} if this set changed. */
    public boolean addAll(LongHashSet other) {
        Objects.requireNonNull(other, "other");
        return addAll(other.toArray());
    }

    public boolean containsAll(long[] values) {
        Objects.requireNonNull(values, "values");
        for (long v : values) if (!contains(v)) return false;
        return true;
    }

    /** Removes every value; returns {@code true} if the set changed. */
    public boolean removeAll(long[] values) {
        Objects.requireNonNull(values, "values");
        boolean changed = false;
        for (long v : values) changed |= remove(v);
        return changed;
    }

    /** Removes every element of {@code other}; returns {@code true} if this set changed. */
    public boolean removeAll(LongHashSet other) {
        Objects.requireNonNull(other, "other");
        return removeAll(other.toArray());
    }

    /** Keeps only the elements that are also in {@code other}; returns {@code true} if this set changed. */
    public boolean retainAll(LongHashSet other) {
        Objects.requireNonNull(other, "other");
        boolean changed = false;
        for (long v : toArray()) {
            if (!other.contains(v)) {
                remove(v);
                changed = true;
            }
        }
        return changed;
    }

    /** Independent deep copy. */
    public LongHashSet copy() { return new LongHashSet(this); }

    private LongHashSet(LongHashSet o) {
        loadFactor = o.loadFactor;
        keys = o.keys.clone();
        mask = o.mask;
        size = o.size;
        threshold = o.threshold;
        hasZero = o.hasZero;
    }

    /** Fail-fast iterator; {@code remove()} is not supported. */
    public PrimitiveIterator.OfLong iterator() { return new Itr(); }

    private final class Itr implements PrimitiveIterator.OfLong {
        private final int expected = modCount;
        private boolean zeroPending = hasZero;
        private int index;

        @Override public boolean hasNext() {
            if (zeroPending) return true;
            long[] ks = keys;
            while (index < ks.length) {
                if (ks[index] != 0L) return true;
                index++;
            }
            return false;
        }

        @Override public long nextLong() {
            if (expected != modCount) throw new ConcurrentModificationException();
            if (zeroPending) {
                zeroPending = false;
                return 0L;
            }
            long[] ks = keys;
            while (index < ks.length) {
                long k = ks[index++];
                if (k != 0L) return k;
            }
            throw new NoSuchElementException();
        }
    }

    /** Snapshot stream; later changes to the set are not reflected. */
    public LongStream stream() { return Arrays.stream(toArray()); }

    /** Boxed snapshot for handing to JDK APIs. */
    public Set<Long> toJavaSet() {
        Set<Long> out = new HashSet<>(HashTables.javaMapCapacity(size()));
        forEach(out::add);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LongHashSet s) || s.size() != size()) return false;
        if (hasZero && !s.hasZero) return false;
        for (long k : keys) if (k != 0L && !s.contains(k)) return false;
        return true;
    }

    /** Same value as {@code Set<Long>.hashCode()} for equal contents. */
    @Override public int hashCode() {
        int h = 0;
        for (long k : keys) h += Long.hashCode(k);
        return h;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("[");
        boolean[] first = {true};
        forEach(k -> {
            if (!first[0]) sb.append(", ");
            first[0] = false;
            sb.append(k);
        });
        return sb.append(']').toString();
    }
}
