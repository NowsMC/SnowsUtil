package space.nows.util.data;

import space.nows.util.check.Preconditions;
import space.nows.util.error.DataException;

import space.nows.util.collections.OrderedObjectMap;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Mutable JSON object that keeps insertion order. Not thread-safe: guard a shared instance externally, or copy it
 * before sharing across threads.
 */
public final class Obj implements Value, Iterable<Map.Entry<String, Value>> {
    private final OrderedObjectMap<String, Value> values;

    public Obj() {
        values = new OrderedObjectMap<>();
    }

    public Obj(int initialCapacity) {
        values = new OrderedObjectMap<>(Preconditions.nonNegative(initialCapacity, "initialCapacity"));
    }

    @Override public Kind type() { return Kind.OBJECT; }

    @Override public Obj asObject() { return this; }

    public Obj put(String key, Value value) {
        values.put(Preconditions.notNull(key, "key"), Preconditions.notNull(value, "value"));
        return this;
    }

    public Obj put(String key, String value) { return put(key, new Str(value)); }

    public Obj put(String key, long value) { return put(key, Num.of(value)); }

    public Obj put(String key, double value) { return put(key, Num.of(value)); }

    public Obj put(String key, boolean value) { return put(key, Bool.of(value)); }

    public Obj putNull(String key) { return put(key, Null.INSTANCE); }

    /** Returns {@code null} when the key is absent. */
    public Value get(String key) { return values.get(key); }

    public Value getOr(String key, Value fallback) { return values.getOrDefault(key, fallback); }

    public String getString(String key) { return required(key).asString(); }

    public Num getNumber(String key) { return required(key).asNumber(); }

    public long getLong(String key) { return getNumber(key).asLong(); }

    public int getInt(String key) { return getNumber(key).asInt(); }

    public double getDouble(String key) { return getNumber(key).asDouble(); }

    public boolean getBoolean(String key) { return required(key).asBoolean(); }

    public Obj getObject(String key) { return required(key).asObject(); }

    public Arr getArray(String key) { return required(key).asArray(); }

    public boolean has(String key) { return values.containsKey(key); }

    /** Returns the removed value, or {@code null} when the key is absent. */
    public Value remove(String key) { return values.remove(key); }

    public int size() { return values.size(); }

    public boolean isEmpty() { return values.isEmpty(); }

    public Set<String> keys() { return Collections.unmodifiableSet(values.keySet()); }

    /** Read-only view of the entries in insertion order. */
    public Map<String, Value> asMap() { return Collections.unmodifiableMap(values); }

    private Value required(String key) {
        Value value = values.get(key);
        if (value == null) throw new DataException("missing object property: \"" + key + "\"");
        return value;
    }

    @Override public Iterator<Map.Entry<String, Value>> iterator() { return asMap().entrySet().iterator(); }

    @Override public boolean equals(Object other) {
        return other instanceof Obj object && values.equals(object.values);
    }

    @Override public int hashCode() { return values.hashCode(); }

    @Override public String toString() { return Debug.of(this); }
}
