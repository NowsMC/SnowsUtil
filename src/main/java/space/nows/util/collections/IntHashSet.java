package space.nows.util.collections;

import java.util.Arrays;
import java.util.function.IntConsumer;

public class IntHashSet {
    private final float loadFactor;
    private int[] keys;
    private int mask;
    private int size;
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
            hasZero = true;
            return added;
        }
        int pos = Hash.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) return false;
            pos = (pos + 1) & mask;
        }
        keys[pos] = key;
        if (++size >= threshold) rehash(keys.length << 1);
        return true;
    }

    public boolean remove(int key) {
        if (key == 0) {
            boolean had = hasZero;
            hasZero = false;
            return had;
        }
        int pos = Hash.mix(key) & mask;
        int k;
        while ((k = keys[pos]) != 0) {
            if (k == key) {
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
                boolean movable = last <= pos ? (last >= slot || slot > pos) : (last >= slot && slot > pos);
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
        size = 0;
        hasZero = false;
    }

    public void forEach(IntConsumer action) {
        if (hasZero) action.accept(0);
        for (int k : keys) if (k != 0) action.accept(k);
    }

    public int[] toArray() {
        int[] out = new int[size()];
        int n = 0;
        if (hasZero) out[n++] = 0;
        for (int k : keys) if (k != 0) out[n++] = k;
        return out;
    }
}
