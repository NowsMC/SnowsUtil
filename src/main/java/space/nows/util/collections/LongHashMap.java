package space.nows.util.collections;

import java.util.Arrays;

public class LongHashMap<V> {
    @FunctionalInterface
    public interface Consumer<V> { void accept(long key, V value); }

    private final float loadFactor;
    private long[] keys;
    private Object[] values;
    private int mask;
    private int size;
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
        keys[pos] = key;
        values[pos] = value;
        if (++size >= threshold) rehash(keys.length << 1);
        return null;
    }

    @SuppressWarnings("unchecked")
    public V remove(long key) {
        if (key == 0L) {
            if (!hasZero) return null;
            V old = zeroValue;
            hasZero = false;
            zeroValue = null;
            return old;
        }
        int pos = find(key);
        if (pos < 0) return null;
        V old = (V) values[pos];
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
                boolean movable = last <= pos ? (last >= slot || slot > pos) : (last >= slot && slot > pos);
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
        size = 0;
        hasZero = false;
        zeroValue = null;
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super V> action) {
        if (hasZero) action.accept(0L, zeroValue);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != 0L) action.accept(keys[i], (V) values[i]);
        }
    }
}
