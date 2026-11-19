package space.nows.util.collections;

import java.util.Arrays;

public class IntObjectMap<V> {
    @FunctionalInterface
    public interface Consumer<V> { void accept(int key, V value); }

    private final float loadFactor;
    private int[] keys;
    private Object[] values;
    private int mask;
    private int size;
    private int threshold;
    private boolean hasZero;
    private V zeroValue;

    public IntObjectMap() { this(16); }

    public IntObjectMap(int expectedSize) { this(expectedSize, 0.6f); }

    public IntObjectMap(int expectedSize, float loadFactor) {
        Hash.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        int cap = Hash.capacityFor(expectedSize, loadFactor);
        keys = new int[cap];
        values = new Object[cap];
        mask = cap - 1;
        threshold = Hash.threshold(cap, loadFactor);
    }

    public int size() { return size + (hasZero ? 1 : 0); }

    public boolean isEmpty() { return size() == 0; }

    private int find(int key) {
        int pos = Hash.mix(key) & mask;
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

    public V getOrDefault(int key, V defaultValue) {
        if (key == 0) return hasZero ? zeroValue : defaultValue;
        int pos = find(key);
        return pos >= 0 ? get(key) : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public V put(int key, V value) {
        if (key == 0) {
            V old = hasZero ? zeroValue : null;
            hasZero = true;
            zeroValue = value;
            return old;
        }
        int pos = Hash.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
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
    public V remove(int key) {
        if (key == 0) {
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
        int[] k = keys;
        Object[] v = values;
        int last, curr, slot;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == 0) { k[last] = 0; v[last] = null; return; }
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
        int[] ok = keys;
        Object[] ov = values;
        keys = new int[newCap];
        values = new Object[newCap];
        mask = newCap - 1;
        threshold = Hash.threshold(newCap, loadFactor);
        for (int i = 0; i < ok.length; i++) {
            int k = ok[i];
            if (k != 0) {
                int pos = Hash.mix(k) & mask;
                while (keys[pos] != 0) pos = (pos + 1) & mask;
                keys[pos] = k;
                values[pos] = ov[i];
            }
        }
    }

    public void clear() {
        Arrays.fill(keys, 0);
        Arrays.fill(values, null);
        size = 0;
        hasZero = false;
        zeroValue = null;
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super V> action) {
        if (hasZero) action.accept(0, zeroValue);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != 0) action.accept(keys[i], (V) values[i]);
        }
    }
}
