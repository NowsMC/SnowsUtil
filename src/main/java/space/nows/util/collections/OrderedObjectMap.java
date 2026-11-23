package space.nows.util.collections;

import space.nows.util.check.Preconditions;
import space.nows.util.error.CapacityExceededException;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Hash map that iterates in insertion order. Entries sit in parallel arrays in insertion order and an open-addressing
 * index points into them, so there is no node object per entry. A removed entry leaves a gap that is reclaimed when the
 * arrays are rebuilt. Map entries returned by iteration stay valid until the next structural change. Not thread-safe.
 */
public class OrderedObjectMap<K, V> extends AbstractMap<K, V> {
    private static final Object DELETED = new Object();
    private static final int MIN_CAPACITY = 8;
    private static final int MAX_CAPACITY = 1 << 29;

    private Object[] keys;
    private Object[] values;
    private int[] hashes;
    private int[] index;
    private int used;
    private int size;
    private int modCount;

    public OrderedObjectMap() {
        this(0);
    }

    public OrderedObjectMap(int expectedSize) {
        Preconditions.nonNegative(expectedSize, "expectedSize");
        allocate(capacityFor(expectedSize));
    }

    public OrderedObjectMap(Map<? extends K, ? extends V> source) {
        this(Preconditions.notNull(source, "source").size());
        putAll(source);
    }

    private static int capacityFor(int minimum) {
        if (minimum > MAX_CAPACITY) throw new CapacityExceededException("ordered map", minimum, MAX_CAPACITY);
        int cap = MIN_CAPACITY;
        while (cap < minimum) cap <<= 1;
        return cap;
    }

    private void allocate(int capacity) {
        keys = new Object[capacity];
        values = new Object[capacity];
        hashes = new int[capacity];
        index = new int[capacity * 2];
        used = 0;
    }

    private static int hash(Object key) {
        int h = key == null ? 0 : key.hashCode();
        return h ^ (h >>> 16);
    }

    private int find(Object key) {
        int h = hash(key);
        int mask = index.length - 1;
        for (int i = h & mask; ; i = (i + 1) & mask) {
            int slot = index[i];
            if (slot == 0) return -1;
            int pos = slot - 1;
            if (hashes[pos] == h && keys[pos] != DELETED && Objects.equals(keys[pos], key)) return pos;
        }
    }

    private void insertIndex(int pos, int h) {
        int mask = index.length - 1;
        int i = h & mask;
        while (index[i] != 0) i = (i + 1) & mask;
        index[i] = pos + 1;
    }

    private void append(Object key, Object value) {
        int pos = used++;
        int h = hash(key);
        keys[pos] = key;
        values[pos] = value;
        hashes[pos] = h;
        insertIndex(pos, h);
        size++;
    }

    private void rebuild(int minimumCapacity) {
        Object[] oldKeys = keys;
        Object[] oldValues = values;
        int[] oldHashes = hashes;
        int oldUsed = used;
        allocate(capacityFor(minimumCapacity));
        for (int p = 0; p < oldUsed; p++) {
            if (oldKeys[p] != DELETED) {
                int pos = used++;
                keys[pos] = oldKeys[p];
                values[pos] = oldValues[p];
                hashes[pos] = oldHashes[p];
                insertIndex(pos, oldHashes[p]);
            }
        }
    }

    @Override public int size() { return size; }

    @Override public boolean isEmpty() { return size == 0; }

    @Override public boolean containsKey(Object key) { return find(key) >= 0; }

    @Override @SuppressWarnings("unchecked")
    public V get(Object key) {
        int pos = find(key);
        return pos < 0 ? null : (V) values[pos];
    }

    @Override @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        int pos = find(key);
        if (pos >= 0) {
            V old = (V) values[pos];
            values[pos] = value;
            return old;
        }
        if (used == keys.length) rebuild(Math.max(MIN_CAPACITY, (size + 1) * 2));
        append(key, value);
        modCount++;
        return null;
    }

    @Override @SuppressWarnings("unchecked")
    public V remove(Object key) {
        int pos = find(key);
        return pos < 0 ? null : removeAt(pos);
    }

    @SuppressWarnings("unchecked")
    private V removeAt(int pos) {
        V old = (V) values[pos];
        keys[pos] = DELETED;
        values[pos] = null;
        size--;
        modCount++;
        return old;
    }

    @Override public void clear() {
        allocate(MIN_CAPACITY);
        size = 0;
        modCount++;
    }

    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        Preconditions.notNull(action, "action");
        int expected = modCount;
        for (int p = 0; p < used; p++) {
            if (keys[p] != DELETED) {
                @SuppressWarnings("unchecked") K key = (K) keys[p];
                @SuppressWarnings("unchecked") V value = (V) values[p];
                action.accept(key, value);
                if (modCount != expected) throw new ConcurrentModificationException();
            }
        }
    }

    @Override public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return OrderedObjectMap.this.size; }

            @Override public Iterator<Map.Entry<K, V>> iterator() { return new EntryIterator(); }

            @Override public void clear() { OrderedObjectMap.this.clear(); }
        };
    }

    private final class EntryIterator implements Iterator<Map.Entry<K, V>> {
        private int next = advance(0);
        private int last = -1;
        private int expectedModCount = modCount;

        private int advance(int from) {
            int p = from;
            while (p < used && keys[p] == DELETED) p++;
            return p;
        }

        @Override public boolean hasNext() { return next < used; }

        @Override public Map.Entry<K, V> next() {
            checkModCount();
            if (next >= used) throw new NoSuchElementException();
            last = next;
            next = advance(next + 1);
            return new EntryView(last);
        }

        @Override public void remove() {
            if (last < 0) throw new IllegalStateException("no entry to remove");
            checkModCount();
            removeAt(last);
            last = -1;
            expectedModCount = modCount;
        }

        private void checkModCount() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    private final class EntryView implements Map.Entry<K, V> {
        private final int pos;
        private final K key;

        EntryView(int pos) {
            this.pos = pos;
            @SuppressWarnings("unchecked") K k = (K) keys[pos];
            this.key = k;
        }

        @Override public K getKey() { return key; }

        @Override @SuppressWarnings("unchecked") public V getValue() { return (V) values[pos]; }

        @Override @SuppressWarnings("unchecked") public V setValue(V value) {
            V old = (V) values[pos];
            values[pos] = value;
            return old;
        }

        @Override public boolean equals(Object other) {
            return other instanceof Map.Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(getValue(), e.getValue());
        }

        @Override public int hashCode() { return Objects.hashCode(key) ^ Objects.hashCode(getValue()); }

        @Override public String toString() { return key + "=" + getValue(); }
    }
}
