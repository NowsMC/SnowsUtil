package space.nows.util.collections;

import java.util.Arrays;

public class IntIntMap {
    @FunctionalInterface
    public interface Consumer { void accept(int key, int value); }

    private final float loadFactor;
    private final int noEntryValue;
    private int[] keys;
    private int[] values;
    private int mask;
    private int size;
    private int threshold;
    private boolean hasZero;
    private int zeroValue;

    public IntIntMap() { this(16); }

    public IntIntMap(int expectedSize) { this(expectedSize, 0.6f, 0); }

    public IntIntMap(int expectedSize, float loadFactor, int noEntryValue) {
        Hash.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        this.noEntryValue = noEntryValue;
        int cap = Hash.capacityFor(expectedSize, loadFactor);
        keys = new int[cap];
        values = new int[cap];
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

    public int get(int key) { return getOrDefault(key, noEntryValue); }

    public int getOrDefault(int key, int defaultValue) {
        if (key == 0) return hasZero ? zeroValue : defaultValue;
        int pos = find(key);
        return pos >= 0 ? values[pos] : defaultValue;
    }

    public int put(int key, int value) {
        if (key == 0) {
            int old = hasZero ? zeroValue : noEntryValue;
            hasZero = true;
            zeroValue = value;
            return old;
        }
        int pos = Hash.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) {
                int old = values[pos];
                values[pos] = value;
                return old;
            }
            pos = (pos + 1) & mask;
        }
        keys[pos] = key;
        values[pos] = value;
        if (++size >= threshold) rehash(keys.length << 1);
        return noEntryValue;
    }

    public int addTo(int key, int delta) {
        if (key == 0) {
            zeroValue = (hasZero ? zeroValue : 0) + delta;
            hasZero = true;
            return zeroValue;
        }
        int pos = Hash.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) return values[pos] += delta;
            pos = (pos + 1) & mask;
        }
        keys[pos] = key;
        values[pos] = delta;
        if (++size >= threshold) rehash(keys.length << 1);
        return delta;
    }

    public int remove(int key) {
        if (key == 0) {
            if (!hasZero) return noEntryValue;
            hasZero = false;
            int old = zeroValue;
            zeroValue = 0;
            return old;
        }
        int pos = find(key);
        if (pos < 0) return noEntryValue;
        int old = values[pos];
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
        int[] ok = keys, ov = values;
        keys = new int[newCap];
        values = new int[newCap];
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
        size = 0;
        hasZero = false;
        zeroValue = 0;
    }

    public void forEach(Consumer action) {
        if (hasZero) action.accept(0, zeroValue);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != 0) action.accept(keys[i], values[i]);
        }
    }

    public int[] keys() {
        int[] out = new int[size()];
        int n = 0;
        if (hasZero) out[n++] = 0;
        for (int k : keys) if (k != 0) out[n++] = k;
        return out;
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
