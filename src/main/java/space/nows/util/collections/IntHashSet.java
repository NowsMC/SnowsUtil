package space.nows.util.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class IntHashSet {
    private final float loadFactor;
    private int[] keys;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
    private boolean hasZero;

    public IntHashSet() { this(16); }

    public IntHashSet(int expectedSize) { this(expectedSize, 0.6f); }

    public IntHashSet(int expectedSize, float loadFactor) {
        Hash.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        int cap = Hash.capacityFor(expectedSize, loadFactor);
        keys = new int[cap];
        mask = cap - 1;
        threshold = Hash.threshold(cap, loadFactor);
    }

    public int size() { return size + (hasZero ? 1 : 0); }

    public boolean isEmpty() { return size() == 0; }

    public boolean contains(int key) {
        if (key == 0) return hasZero;
        int pos = Hash.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) return true;
            pos = (pos + 1) & mask;
        }
        return false;
    }

    public boolean add(int key) {
        if (key == 0) {
            boolean added = !hasZero;
            if (!hasZero) modCount++;
            hasZero = true;
            return added;
        }
        int pos = Hash.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) return false;
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        if (++size >= threshold) rehash(Hash.grow(keys.length));
        return true;
    }

    public boolean remove(int key) {
        if (key == 0) {
            boolean had = hasZero;
            if (hasZero) modCount++;
            hasZero = false;
            return had;
        }
        int pos = Hash.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) {
                modCount++;
                shiftDelete(pos);
                size--;
                return true;
            }
            pos = (pos + 1) & mask;
        }
        return false;
    }

    private void shiftDelete(int pos) {
        int[] k = keys;
        int last, curr, slot;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == 0) { k[last] = 0; return; }
                slot = Hash.mix(curr) & mask;
                boolean movable = Hash.isMovable(last, pos, slot);
                if (movable) break;
                pos = (pos + 1) & mask;
            }
            k[last] = curr;
        }
    }

    private void rehash(int newCap) {
        int[] ok = keys;
        keys = new int[newCap];
        mask = newCap - 1;
        threshold = Hash.threshold(newCap, loadFactor);
        for (int k : ok) {
            if (k != 0) {
                int pos = Hash.mix(k) & mask;
                while (keys[pos] != 0) pos = (pos + 1) & mask;
                keys[pos] = k;
            }
        }
    }

    public void clear() {
        Arrays.fill(keys, 0);
        modCount++;
        size = 0;
        if (hasZero) modCount++;
        hasZero = false;
    }

    public void forEach(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        if (hasZero) {
            action.accept(0);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
        int[] ks = keys;
        for (int i = 0; i < ks.length; i++) {
            if (ks[i] != 0) {
                action.accept(ks[i]);
                if (mc != modCount) throw new ConcurrentModificationException();
            }
        }
    }

    public int[] toArray() {
        int[] out = new int[size()];
        int n = 0;
        if (hasZero) out[n++] = 0;
        for (int k : keys) if (k != 0) out[n++] = k;
        return out;
    }

    public static IntHashSet of(int... values) {
        Objects.requireNonNull(values, "values");
        IntHashSet set = new IntHashSet(values.length);
        set.addAll(values);
        return set;
    }

    /** Adds every value; returns {@code true} if the set changed. */
    public boolean addAll(int[] values) {
        Objects.requireNonNull(values, "values");
        boolean changed = false;
        for (int v : values) changed |= add(v);
        return changed;
    }

    public boolean containsAll(int[] values) {
        Objects.requireNonNull(values, "values");
        for (int v : values) if (!contains(v)) return false;
        return true;
    }

    /** Independent deep copy. */
    public IntHashSet copy() { return new IntHashSet(this); }

    private IntHashSet(IntHashSet o) {
        loadFactor = o.loadFactor;
        keys = o.keys.clone();
        mask = o.mask;
        size = o.size;
        threshold = o.threshold;
        hasZero = o.hasZero;
    }

    /** Fail-fast iterator; {@code remove()} is not supported. */
    public PrimitiveIterator.OfInt iterator() { return new Itr(); }

    private final class Itr implements PrimitiveIterator.OfInt {
        private final int expected = modCount;
        private boolean zeroPending = hasZero;
        private int index;

        @Override public boolean hasNext() {
            if (zeroPending) return true;
            int[] ks = keys;
            while (index < ks.length) {
                if (ks[index] != 0) return true;
                index++;
            }
            return false;
        }

        @Override public int nextInt() {
            if (expected != modCount) throw new ConcurrentModificationException();
            if (zeroPending) {
                zeroPending = false;
                return 0;
            }
            int[] ks = keys;
            while (index < ks.length) {
                int k = ks[index++];
                if (k != 0) return k;
            }
            throw new NoSuchElementException();
        }
    }

    /** Snapshot stream; later changes to the set are not reflected. */
    public IntStream stream() { return Arrays.stream(toArray()); }

    /** Boxed snapshot for handing to JDK APIs. */
    public Set<Integer> toJavaSet() {
        Set<Integer> out = new HashSet<>(Hash.javaMapCapacity(size()));
        forEach(out::add);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof IntHashSet s) || s.size() != size()) return false;
        if (hasZero && !s.hasZero) return false;
        for (int k : keys) if (k != 0 && !s.contains(k)) return false;
        return true;
    }

    /** Same value as {@code Set<Integer>.hashCode()} for equal contents. */
    @Override public int hashCode() {
        int h = 0;
        for (int k : keys) h += k;
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
