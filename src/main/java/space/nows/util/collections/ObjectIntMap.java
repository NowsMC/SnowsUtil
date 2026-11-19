package space.nows.util.collections;

import java.util.Arrays;
import java.util.Objects;

public class ObjectIntMap<K> {
    @FunctionalInterface
    public interface Consumer<K> { void accept(K key, int value); }

    private final float loadFactor;
    private final int noEntryValue;
    private Object[] keys;
    private int[] values;
    private int mask;
    private int size;
    private int threshold;

    public ObjectIntMap() { this(16); }

    public ObjectIntMap(int expectedSize) { this(expectedSize, 0.6f, 0); }

    public ObjectIntMap(int expectedSize, float loadFactor, int noEntryValue) {
        Hash.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        this.noEntryValue = noEntryValue;
        int cap = Hash.capacityFor(expectedSize, loadFactor);
        keys = new Object[cap];
        values = new int[cap];
        mask = cap - 1;
        threshold = Hash.threshold(cap, loadFactor);
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    private static int hash(Object key) { return Hash.mix(key.hashCode()); }

    private int find(Object key) {
        int pos = hash(key) & mask;
        Object k;
        while ((k = keys[pos]) != null) {
            if (k == key || k.equals(key)) return pos;
            pos = (pos + 1) & mask;
        }
        return -1;
    }

    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "key");
        return find(key) >= 0;
    }

    public int get(K key) { return getOrDefault(key, noEntryValue); }

    public int getOrDefault(K key, int defaultValue) {
        Objects.requireNonNull(key, "key");
        int pos = find(key);
        return pos >= 0 ? values[pos] : defaultValue;
    }

    public int put(K key, int value) {
        Objects.requireNonNull(key, "key");
        int pos = hash(key) & mask;
        Object k;
        while ((k = keys[pos]) != null) {
            if (k == key || k.equals(key)) {
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

    public int addTo(K key, int delta) {
        Objects.requireNonNull(key, "key");
        int pos = hash(key) & mask;
        Object k;
        while ((k = keys[pos]) != null) {
            if (k == key || k.equals(key)) return values[pos] += delta;
            pos = (pos + 1) & mask;
        }
        keys[pos] = key;
        values[pos] = delta;
        if (++size >= threshold) rehash(keys.length << 1);
        return delta;
    }

    public int remove(K key) {
        Objects.requireNonNull(key, "key");
        int pos = find(key);
        if (pos < 0) return noEntryValue;
        int old = values[pos];
        shiftDelete(pos);
        size--;
        return old;
    }

    private void shiftDelete(int pos) {
        Object[] k = keys;
        int[] v = values;
        int last, slot;
        Object curr;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == null) { k[last] = null; return; }
                slot = hash(curr) & mask;
                boolean movable = last <= pos ? (last >= slot || slot > pos) : (last >= slot && slot > pos);
                if (movable) break;
                pos = (pos + 1) & mask;
            }
            k[last] = curr;
            v[last] = v[pos];
        }
    }

    private void rehash(int newCap) {
        Object[] ok = keys;
        int[] ov = values;
        keys = new Object[newCap];
        values = new int[newCap];
        mask = newCap - 1;
        threshold = Hash.threshold(newCap, loadFactor);
        for (int i = 0; i < ok.length; i++) {
            Object k = ok[i];
            if (k != null) {
                int pos = hash(k) & mask;
                while (keys[pos] != null) pos = (pos + 1) & mask;
                keys[pos] = k;
                values[pos] = ov[i];
            }
        }
    }

    public void clear() {
        Arrays.fill(keys, null);
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super K> action) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) action.accept((K) keys[i], values[i]);
        }
    }
}
