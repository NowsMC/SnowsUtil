package space.nows.util.json;

import space.nows.util.check.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Mutable JSON array. */
public final class JsonArray implements JsonValue, Iterable<JsonValue> {
    private final ArrayList<JsonValue> values;

    public JsonArray() {
        values = new ArrayList<>();
    }

    public JsonArray(int initialCapacity) {
        values = new ArrayList<>(Preconditions.nonNegative(initialCapacity, "initialCapacity"));
    }

    @Override public JsonType type() { return JsonType.ARRAY; }

    @Override public JsonArray asArray() { return this; }

    public JsonArray add(JsonValue value) {
        values.add(Preconditions.notNull(value, "value"));
        return this;
    }

    public JsonArray add(String value) { return add(new JsonString(value)); }

    public JsonArray add(long value) { return add(JsonNumber.of(value)); }

    public JsonArray add(double value) { return add(JsonNumber.of(value)); }

    public JsonArray add(boolean value) { return add(JsonBoolean.of(value)); }

    public JsonArray addNull() { return add(JsonNull.INSTANCE); }

    public JsonValue get(int index) { return values.get(index); }

    public int size() { return values.size(); }

    public boolean isEmpty() { return values.isEmpty(); }

    /** Read-only view of the elements. */
    public List<JsonValue> asList() { return Collections.unmodifiableList(values); }

    @Override public Iterator<JsonValue> iterator() { return asList().iterator(); }

    @Override public boolean equals(Object other) {
        return other instanceof JsonArray array && values.equals(array.values);
    }

    @Override public int hashCode() { return values.hashCode(); }

    @Override public String toString() { return Json.write(this); }
}
