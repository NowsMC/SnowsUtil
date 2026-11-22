package space.nows.util.collections;

$IMPORTS$

/**
 * Open-addressing hash set of {@code $K$} values, with no per-element objects. Not thread-safe; iteration is
 * fail-fast. Generated from {@code HashSet.tpl}; edit the template, not this file.
 */
public class $CLASS$ {
    private final float loadFactor;
    private $K$[] keys;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
    private boolean hasZero;

    public $CLASS$() { this(16); }

    public $CLASS$(int expectedSize) { this(expectedSize, 0.6f); }

    public $CLASS$(int expectedSize, float loadFactor) {
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new $K$[cap];
        mask = cap - 1;
        threshold = HashTables.threshold(cap, loadFactor);
    }

    public int size() { return size + (hasZero ? 1 : 0); }

    public boolean isEmpty() { return size() == 0; }

    private int find($K$ key) {
        int pos = HashTables.mix(key) & mask;
        $K$ k;
        while ((k = keys[pos]) != $ZERO$) {
            if (k == key) return pos;
            pos = (pos + 1) & mask;
        }
        return -1;
    }

    public boolean contains($K$ key) {
        return key == $ZERO$ ? hasZero : find(key) >= 0;
    }

    public boolean add($K$ key) {
        if (key == $ZERO$) {
            boolean added = !hasZero;
            if (added) modCount++;
            hasZero = true;
            return added;
        }
        int pos = HashTables.mix(key) & mask;
        $K$ k;
        while ((k = keys[pos]) != $ZERO$) {
            if (k == key) return false;
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        if (++size >= threshold) rehash(HashTables.grow(keys.length));
        return true;
    }

    public boolean remove($K$ key) {
        if (key == $ZERO$) {
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
        $K$[] k = keys;
        int last, slot;
        $K$ curr;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == $ZERO$) {
                    k[last] = $ZERO$;
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
        $K$[] ok = keys;
        keys = new $K$[newCap];
        mask = newCap - 1;
        threshold = HashTables.threshold(newCap, loadFactor);
        for ($K$ k : ok) {
            if (k != $ZERO$) {
                int pos = HashTables.mix(k) & mask;
                while (keys[pos] != $ZERO$) pos = (pos + 1) & mask;
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
        if (size > 0) Arrays.fill(keys, $ZERO$);
        modCount++;
        size = 0;
        if (hasZero) modCount++;
        hasZero = false;
    }

    public void forEach($CONSUMER$ action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
        if (hasZero) {
            action.accept($ZERO$);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
        $K$[] ks = keys;
        for (int i = 0; i < ks.length; i++) {
            if (ks[i] != $ZERO$) {
                action.accept(ks[i]);
                if (mc != modCount) throw new ConcurrentModificationException();
            }
        }
    }

    public $K$[] toArray() {
        $K$[] out = new $K$[size()];
        int n = 0;
        if (hasZero) out[n++] = $ZERO$;
        for ($K$ k : keys) if (k != $ZERO$) out[n++] = k;
        return out;
    }

    public static $CLASS$ of($K$... values) {
        Objects.requireNonNull(values, "values");
        $CLASS$ set = new $CLASS$(values.length);
        set.addAll(values);
        return set;
    }

    /** Adds every value; returns {@code true} if the set changed. */
    public boolean addAll($K$[] values) {
        Objects.requireNonNull(values, "values");
        boolean changed = false;
        for ($K$ v : values) changed |= add(v);
        return changed;
    }

    /** Adds every element of {@code other}; returns {@code true} if this set changed. */
    public boolean addAll($CLASS$ other) {
        Objects.requireNonNull(other, "other");
        return addAll(other.toArray());
    }

    public boolean containsAll($K$[] values) {
        Objects.requireNonNull(values, "values");
        for ($K$ v : values) if (!contains(v)) return false;
        return true;
    }

    /** Removes every value; returns {@code true} if the set changed. */
    public boolean removeAll($K$[] values) {
        Objects.requireNonNull(values, "values");
        boolean changed = false;
        for ($K$ v : values) changed |= remove(v);
        return changed;
    }

    /** Removes every element of {@code other}; returns {@code true} if this set changed. */
    public boolean removeAll($CLASS$ other) {
        Objects.requireNonNull(other, "other");
        return removeAll(other.toArray());
    }

    /** Keeps only the elements that are also in {@code other}; returns {@code true} if this set changed. */
    public boolean retainAll($CLASS$ other) {
        Objects.requireNonNull(other, "other");
        boolean changed = false;
        for ($K$ v : toArray()) {
            if (!other.contains(v)) {
                remove(v);
                changed = true;
            }
        }
        return changed;
    }

    /** Independent deep copy. */
    public $CLASS$ copy() { return new $CLASS$(this); }

    private $CLASS$($CLASS$ o) {
        loadFactor = o.loadFactor;
        keys = o.keys.clone();
        mask = o.mask;
        size = o.size;
        threshold = o.threshold;
        hasZero = o.hasZero;
    }

    /** Fail-fast iterator; {@code remove()} is not supported. */
    public PrimitiveIterator.$ITER$ iterator() { return new Itr(); }

    private final class Itr implements PrimitiveIterator.$ITER$ {
        private final int expected = modCount;
        private boolean zeroPending = hasZero;
        private int index;

        @Override public boolean hasNext() {
            if (zeroPending) return true;
            $K$[] ks = keys;
            while (index < ks.length) {
                if (ks[index] != $ZERO$) return true;
                index++;
            }
            return false;
        }

        @Override public $K$ $NEXT$() {
            if (expected != modCount) throw new ConcurrentModificationException();
            if (zeroPending) {
                zeroPending = false;
                return $ZERO$;
            }
            $K$[] ks = keys;
            while (index < ks.length) {
                $K$ k = ks[index++];
                if (k != $ZERO$) return k;
            }
            throw new NoSuchElementException();
        }
    }

    /** Snapshot stream; later changes to the set are not reflected. */
    public $STREAM$ stream() { return Arrays.stream(toArray()); }

    /** Boxed snapshot for handing to JDK APIs. */
    public Set<$BOX$> toJavaSet() {
        Set<$BOX$> out = new HashSet<>(HashTables.javaMapCapacity(size()));
        forEach(out::add);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof $CLASS$ s) || s.size() != size()) return false;
        if (hasZero && !s.hasZero) return false;
        for ($K$ k : keys) if (k != $ZERO$ && !s.contains(k)) return false;
        return true;
    }

    /** Same value as {@code Set<$BOX$>.hashCode()} for equal contents. */
    @Override public int hashCode() {
        int h = 0;
        for ($K$ k : keys) h += @KHASH(k);
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
