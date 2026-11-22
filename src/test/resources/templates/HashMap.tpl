package space.nows.util.collections;

$IMPORTS$

/**
 * Open-addressing hash map from {@code $KDOC$} keys to {@code $VDOC$} values, with no per-entry objects.
$NOENTRYDOC$ * Not thread-safe; iteration is fail-fast. Generated from {@code HashMap.tpl}; edit the template, not this file.
 */
$SUPPRESS$
public class $CLASS$$GEN$ {
    private final float loadFactor;
#if VAL_PRIM
    private final $VPUB$ noEntryValue;
#end
    private $KT$[] keys;
    private $VT$[] values;
    private int mask;
    private int size;
    private int modCount;
    private int threshold;
#if KEY_PRIM
    private boolean hasZero;
    private $VPUB$ zeroValue;
#end

    public $CLASS$() { this(16); }

#if VAL_PRIM
    public $CLASS$(int expectedSize) { this(expectedSize, 0.6f, $VCLEAR$); }

    public $CLASS$(int expectedSize, float loadFactor, $VPUB$ noEntryValue) {
#else
    public $CLASS$(int expectedSize) { this(expectedSize, 0.6f); }

    public $CLASS$(int expectedSize, float loadFactor) {
#end
        HashTables.checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
#if VAL_PRIM
        this.noEntryValue = noEntryValue;
#end
        int cap = HashTables.capacityFor(expectedSize, loadFactor);
        keys = new $KT$[cap];
        values = new $VT$[cap];
        mask = cap - 1;
        threshold = HashTables.threshold(cap, loadFactor);
    }

#if KEY_PRIM
    public int size() { return size + (hasZero ? 1 : 0); }
#else
    public int size() { return size; }
#end

    public boolean isEmpty() { return size() == 0; }

    private int find($KT$ key) {
        int pos = @HASH(key) & mask;
        $KT$ k;
        while ((k = keys[pos]) != $KEMPTY$) {
            if (@KEQ(k, key)) return pos;
            pos = (pos + 1) & mask;
        }
        return -1;
    }

    public boolean containsKey($KPUB$ key) {
        @NNKEY
#if KEY_PRIM
        return key == $KEMPTY$ ? hasZero : find(key) >= 0;
#else
        return find(key) >= 0;
#end
    }

    public $VPUB$ get($KPUB$ key) { return getOrDefault(key, $VNONE$); }

    public $VPUB$ getOrDefault($KPUB$ key, $VPUB$ defaultValue) {
        @NNKEY
#if KEY_PRIM
        if (key == $KEMPTY$) return hasZero ? zeroValue : defaultValue;
#end
        int pos = find(key);
        return pos >= 0 ? $VCAST$values[pos] : defaultValue;
    }

    public $VPUB$ put($KPUB$ key, $VPUB$ value) {
        @NNKEY
#if KEY_PRIM
        if (key == $KEMPTY$) {
            $VPUB$ old = hasZero ? zeroValue : $VNONE$;
            if (!hasZero) modCount++;
            hasZero = true;
            zeroValue = value;
            return old;
        }
#end
        int pos = @HASH(key) & mask;
        $KT$ k;
        while ((k = keys[pos]) != $KEMPTY$) {
            if (@KEQ(k, key)) {
                $VPUB$ old = $VCAST$values[pos];
                values[pos] = value;
                return old;
            }
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        values[pos] = value;
        if (++size >= threshold) rehash(HashTables.grow(keys.length));
        return $VNONE$;
    }

#if VAL_PRIM
    /** Adds {@code delta} to the value for {@code key}, treating an absent key as 0; returns the new value. */
    public $VPUB$ addTo($KPUB$ key, $VPUB$ delta) {
        @NNKEY
#if KEY_PRIM
        if (key == $KEMPTY$) {
            zeroValue = (hasZero ? zeroValue : $VCLEAR$) + delta;
            if (!hasZero) modCount++;
            hasZero = true;
            return zeroValue;
        }
#end
        int pos = @HASH(key) & mask;
        $KT$ k;
        while ((k = keys[pos]) != $KEMPTY$) {
            if (@KEQ(k, key)) return values[pos] += delta;
            pos = (pos + 1) & mask;
        }
        modCount++;
        keys[pos] = key;
        values[pos] = delta;
        if (++size >= threshold) rehash(HashTables.grow(keys.length));
        return delta;
    }

#end
    public $VPUB$ remove($KPUB$ key) {
        @NNKEY
#if KEY_PRIM
        if (key == $KEMPTY$) {
            if (!hasZero) return $VNONE$;
            modCount++;
            hasZero = false;
            $VPUB$ old = zeroValue;
            zeroValue = $VCLEAR$;
            return old;
        }
#end
        int pos = find(key);
        if (pos < 0) return $VNONE$;
        $VPUB$ old = $VCAST$values[pos];
        modCount++;
        shiftDelete(pos);
        size--;
        return old;
    }

    private void shiftDelete(int pos) {
        $KT$[] k = keys;
        $VT$[] v = values;
        int last, slot;
        $KT$ curr;
        for (;;) {
            last = pos;
            pos = (pos + 1) & mask;
            for (;;) {
                curr = k[pos];
                if (curr == $KEMPTY$) {
                    k[last] = $KEMPTY$;
#if VAL_OBJ
                    v[last] = null;
#end
                    return;
                }
                slot = @HASH(curr) & mask;
                if (HashTables.isMovable(last, pos, slot)) break;
                pos = (pos + 1) & mask;
            }
            k[last] = curr;
            v[last] = v[pos];
        }
    }

    private void rehash(int newCap) {
        $KT$[] ok = keys;
        $VT$[] ov = values;
        keys = new $KT$[newCap];
        values = new $VT$[newCap];
        mask = newCap - 1;
        threshold = HashTables.threshold(newCap, loadFactor);
        for (int i = 0; i < ok.length; i++) {
            $KT$ k = ok[i];
            if (k != $KEMPTY$) {
                int pos = @HASH(k) & mask;
                while (keys[pos] != $KEMPTY$) pos = (pos + 1) & mask;
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
        if (size > 0) {
            Arrays.fill(keys, $KEMPTY$);
#if VAL_OBJ
            Arrays.fill(values, null);
#end
        }
        modCount++;
        size = 0;
#if KEY_PRIM
        if (hasZero) modCount++;
        hasZero = false;
        zeroValue = $VCLEAR$;
#end
    }

    public void forEach($CONSUMER$ action) {
        Objects.requireNonNull(action, "action");
        int mc = modCount;
#if KEY_PRIM
        if (hasZero) {
            action.accept($KEMPTY$, zeroValue);
            if (mc != modCount) throw new ConcurrentModificationException();
        }
#end
        $KT$[] ks = keys;
        $VT$[] vs = values;
        for (int i = 0; i < ks.length; i++) {
            $KT$ k = ks[i];
            if (k != $KEMPTY$) {
                action.accept($KCAST$k, $VCAST$vs[i]);
                if (mc != modCount) throw new ConcurrentModificationException();
            }
        }
    }

#if KEY_PRIM
    public $KPUB$[] keys() {
        $KPUB$[] out = new $KPUB$[size()];
        int n = 0;
        if (hasZero) out[n++] = $KEMPTY$;
        for ($KPUB$ k : keys) if (k != $KEMPTY$) out[n++] = k;
        return out;
    }

#end
#if VAL_PRIM
    /** Values in the same order as {@link #forEach}. */
    public $VPUB$[] values() {
        $VPUB$[] out = new $VPUB$[size()];
        int n = 0;
#if KEY_PRIM
        if (hasZero) out[n++] = zeroValue;
#end
        for (int i = 0; i < keys.length; i++) if (keys[i] != $KEMPTY$) out[n++] = values[i];
        return out;
    }

#end
    /** Stores {@code value} only if {@code key} is absent. Returns the existing value, or $ABSENTDOC$ if it was absent. */
    public $VPUB$ putIfAbsent($KPUB$ key, $VPUB$ value) {
        @NNKEY
#if KEY_PRIM
        if (key == $KEMPTY$) {
            if (hasZero) return zeroValue;
        } else {
            int pos = find(key);
            if (pos >= 0) return $VCAST$values[pos];
        }
#else
        int pos = find(key);
        if (pos >= 0) return $VCAST$values[pos];
#end
        put(key, value);
        return $VNONE$;
    }

#if VAL_OBJ
    /** Returns the value for {@code key}, computing and storing it first if absent. */
    public $VPUB$ computeIfAbsent($KPUB$ key, $MAPPING$ mapping) {
        Objects.requireNonNull(mapping, "mapping");
        @NNKEY
#if KEY_PRIM
        if (key == $KEMPTY$) {
            if (hasZero) return zeroValue;
        } else {
            int pos = find(key);
            if (pos >= 0) return $VCAST$values[pos];
        }
#else
        int pos = find(key);
        if (pos >= 0) return $VCAST$values[pos];
#end
        $VPUB$ created = mapping.apply(key);
        put(key, created);
        return created;
    }

#else
    /** Combines the existing value (if any) with {@code value}; stores and returns the result. */
    public $VPUB$ merge($KPUB$ key, $VPUB$ value, $BINOP$ remapping) {
        Objects.requireNonNull(remapping, "remapping");
        @NNKEY
        $VPUB$ existing;
#if KEY_PRIM
        if (key == $KEMPTY$) {
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
#else
        int pos = find(key);
        if (pos < 0) {
            put(key, value);
            return value;
        }
        existing = values[pos];
#end
        $VPUB$ merged = remapping.$BINAPPLY$(existing, value);
        put(key, merged);
        return merged;
    }

#end
    /** Independent copy of the table$COPYDOC$. */
    public $CLASS$$GEN$ copy() { return new $CLASS$$DIAMOND$(this); }

    private $CLASS$($CLASS$$GEN$ o) {
        loadFactor = o.loadFactor;
#if VAL_PRIM
        noEntryValue = o.noEntryValue;
#end
        keys = o.keys.clone();
        values = o.values.clone();
        mask = o.mask;
        size = o.size;
        threshold = o.threshold;
#if KEY_PRIM
        hasZero = o.hasZero;
        zeroValue = o.zeroValue;
#end
    }

    /** Boxed snapshot for handing to JDK APIs. */
    public Map<$KBOX$, $VBOX$> toJavaMap() {
        Map<$KBOX$, $VBOX$> out = new HashMap<>(HashTables.javaMapCapacity(size()));
        forEach(out::put);
        return out;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof $CLASS$$WILD$ m) || m.size() != size()) return false;
#if KEY_PRIM
        if (hasZero && (!m.hasZero || @VNE(m.zeroValue, zeroValue))) return false;
#end
        for (int i = 0; i < keys.length; i++) {
            $KT$ k = keys[i];
            if (k != $KEMPTY$) {
                int pos = m.find(k);
                if (pos < 0 || @VNE(m.values[pos], values[i])) return false;
            }
        }
        return true;
    }

    /** Same value as {@code Map.hashCode()} of the boxed equivalent for equal contents. */
    @Override public int hashCode() {
        int h = 0;
#if KEY_PRIM
        if (hasZero) h = @VHASH(zeroValue);
#end
        for (int i = 0; i < keys.length; i++) if (keys[i] != $KEMPTY$) h += @KHASH(keys[i]) ^ @VHASH(values[i]);
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
