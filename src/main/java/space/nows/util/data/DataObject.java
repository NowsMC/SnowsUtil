package space.nows.util.data;

import space.nows.util.check.Preconditions;
import space.nows.util.error.DataException;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Mutable JSON object that keeps insertion order. */
public final class DataObject implements DataValue, Iterable<Map.Entry<String, DataValue>> {
    private final LinkedHashMap<String, DataValue> values;

    public DataObject() {
        values = new LinkedHashMap<>();
    }

    public DataObject(int initialCapacity) {
        values = new LinkedHashMap<>(Preconditions.nonNegative(initialCapacity, "initialCapacity"));
    }

    @Override public DataType type() { return DataType.OBJECT; }

    @Override public DataObject asObject() { return this; }

    public DataObject put(String key, DataValue value) {
        values.put(Preconditions.notNull(key, "key"), Preconditions.notNull(value, "value"));
        return this;
    }

    public DataObject put(String key, String value) { return put(key, new DataString(value)); }

    public DataObject put(String key, long value) { return put(key, DataNumber.of(value)); }

    public DataObject put(String key, double value) { return put(key, DataNumber.of(value)); }

    public DataObject put(String key, boolean value) { return put(key, DataBoolean.of(value)); }

    public DataObject putNull(String key) { return put(key, DataNull.INSTANCE); }

    /** Returns {@code null} when the key is absent. */
    public DataValue get(String key) { return values.get(key); }

    public DataValue getOr(String key, DataValue fallback) { return values.getOrDefault(key, fallback); }

    public String getString(String key) { return required(key).asString(); }

    public DataNumber getNumber(String key) { return required(key).asNumber(); }

    public long getLong(String key) { return getNumber(key).asLong(); }

    public int getInt(String key) { return getNumber(key).asInt(); }

    public double getDouble(String key) { return getNumber(key).asDouble(); }

    public boolean getBoolean(String key) { return required(key).asBoolean(); }

    public DataObject getObject(String key) { return required(key).asObject(); }

    public DataArray getArray(String key) { return required(key).asArray(); }

    public boolean has(String key) { return values.containsKey(key); }

    /** Returns the removed value, or {@code null} when the key is absent. */
    public DataValue remove(String key) { return values.remove(key); }

    public int size() { return values.size(); }

    public boolean isEmpty() { return values.isEmpty(); }

    public Set<String> keys() { return Collections.unmodifiableSet(values.keySet()); }

    /** Read-only view of the entries in insertion order. */
    public Map<String, DataValue> asMap() { return Collections.unmodifiableMap(values); }

    private DataValue required(String key) {
        DataValue value = values.get(key);
        if (value == null) throw new DataException("missing object property: \"" + key + "\"");
        return value;
    }

    @Override public Iterator<Map.Entry<String, DataValue>> iterator() { return asMap().entrySet().iterator(); }

    @Override public boolean equals(Object other) {
        return other instanceof DataObject object && values.equals(object.values);
    }

    @Override public int hashCode() { return values.hashCode(); }

    @Override public String toString() { return DataText.of(this); }
}
