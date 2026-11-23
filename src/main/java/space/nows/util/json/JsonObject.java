package space.nows.util.json;

import space.nows.util.check.Preconditions;
import space.nows.util.error.JsonException;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Mutable JSON object that keeps insertion order. */
public final class JsonObject implements JsonValue, Iterable<Map.Entry<String, JsonValue>> {
    private final LinkedHashMap<String, JsonValue> values;

    public JsonObject() {
        values = new LinkedHashMap<>();
    }

    public JsonObject(int initialCapacity) {
        values = new LinkedHashMap<>(Preconditions.nonNegative(initialCapacity, "initialCapacity"));
    }

    @Override public JsonType type() { return JsonType.OBJECT; }

    @Override public JsonObject asObject() { return this; }

    public JsonObject put(String key, JsonValue value) {
        values.put(Preconditions.notNull(key, "key"), Preconditions.notNull(value, "value"));
        return this;
    }

    public JsonObject put(String key, String value) { return put(key, new JsonString(value)); }

    public JsonObject put(String key, long value) { return put(key, JsonNumber.of(value)); }

    public JsonObject put(String key, double value) { return put(key, JsonNumber.of(value)); }

    public JsonObject put(String key, boolean value) { return put(key, JsonBoolean.of(value)); }

    public JsonObject putNull(String key) { return put(key, JsonNull.INSTANCE); }

    /** Returns {@code null} when the key is absent. */
    public JsonValue get(String key) { return values.get(key); }

    public JsonValue getOr(String key, JsonValue fallback) { return values.getOrDefault(key, fallback); }

    public String getString(String key) { return required(key).asString(); }

    public JsonNumber getNumber(String key) { return required(key).asNumber(); }

    public long getLong(String key) { return getNumber(key).asLong(); }

    public int getInt(String key) { return getNumber(key).asInt(); }

    public double getDouble(String key) { return getNumber(key).asDouble(); }

    public boolean getBoolean(String key) { return required(key).asBoolean(); }

    public JsonObject getObject(String key) { return required(key).asObject(); }

    public JsonArray getArray(String key) { return required(key).asArray(); }

    public boolean has(String key) { return values.containsKey(key); }

    /** Returns the removed value, or {@code null} when the key is absent. */
    public JsonValue remove(String key) { return values.remove(key); }

    public int size() { return values.size(); }

    public boolean isEmpty() { return values.isEmpty(); }

    public Set<String> keys() { return Collections.unmodifiableSet(values.keySet()); }

    /** Read-only view of the entries in insertion order. */
    public Map<String, JsonValue> asMap() { return Collections.unmodifiableMap(values); }

    private JsonValue required(String key) {
        JsonValue value = values.get(key);
        if (value == null) throw new JsonException("missing object property: \"" + key + "\"");
        return value;
    }

    @Override public Iterator<Map.Entry<String, JsonValue>> iterator() { return asMap().entrySet().iterator(); }

    @Override public boolean equals(Object other) {
        return other instanceof JsonObject object && values.equals(object.values);
    }

    @Override public int hashCode() { return values.hashCode(); }

    @Override public String toString() { return Json.write(this); }
}
